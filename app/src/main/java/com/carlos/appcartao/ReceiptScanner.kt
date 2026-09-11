package com.carlos.appcartao

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.time.LocalDate

internal data class ReceiptScanResult(
    val amountCents: Long?,
    val purchaseDate: LocalDate?,
    val category: String?,
    val description: String,
    val rawText: String
)

internal class ReceiptOcrScanner(private val context: Context) {
    fun scan(
        uri: Uri,
        categories: List<String>,
        callback: (ReceiptScanResult?, String?) -> Unit
    ) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (_: Exception) {
            callback(null, "Não foi possível abrir a foto do comprovante.")
            return
        }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { recognized ->
                val result = ReceiptParser.parse(recognized.text, categories)
                recognizer.close()
                if (recognized.text.isBlank()) {
                    callback(null, "Não encontrei texto legível. Tente aproximar a câmera e evitar reflexos.")
                } else {
                    callback(result, null)
                }
            }
            .addOnFailureListener {
                recognizer.close()
                callback(null, "Não consegui ler o comprovante. Tente uma foto mais nítida.")
            }
    }
}

internal object ReceiptParser {
    private val amountRegex = Regex(
        "(?<!\\d)(?:R\\$\\s*)?(\\d{1,3}(?:\\.\\d{3})*,\\d{2}|\\d+,\\d{2}|\\d+\\.\\d{2})(?!\\d)",
        RegexOption.IGNORE_CASE
    )
    private val dateRegex = Regex("\\b([0-3]?\\d)[/.-]([01]?\\d)[/.-](\\d{2,4})\\b")

    fun parse(text: String, categories: List<String>): ReceiptScanResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val amount = findTotal(lines)
        val date = findDate(lines)
        val description = findMerchant(lines)
        val category = categorize(text, categories)
        return ReceiptScanResult(amount, date, category, description, text)
    }

    private fun findTotal(lines: List<String>): Long? {
        data class Candidate(val cents: Long, val score: Int, val index: Int)
        val candidates = mutableListOf<Candidate>()
        lines.forEachIndexed { index, original ->
            val line = normalize(original)
            amountRegex.findAll(original).forEach { match ->
                val cents = parseAmount(match.groupValues[1]) ?: return@forEach
                if (cents <= 0 || cents > 1_000_000_000L) return@forEach
                var score = 0
                if (listOf("total a pagar", "valor total", "total geral", "valor pago", "total pago").any { line.contains(it) }) score += 1500
                else if (Regex("\\btotal\\b").containsMatchIn(line)) score += 1100
                else if (listOf("a pagar", "valor da compra", "valor compra", "pago").any { line.contains(it) }) score += 700
                if (line.contains("subtotal")) score -= 500
                if (line.contains("troco")) score -= 900
                if (line.contains("desconto")) score -= 700
                if (line.contains("acrescimo")) score -= 200
                score += index.coerceAtMost(300)
                candidates += Candidate(cents, score, index)
            }
        }
        if (candidates.isEmpty()) return null
        val explicit = candidates.filter { it.score >= 600 }
        return if (explicit.isNotEmpty()) {
            explicit.maxWithOrNull(compareBy<Candidate> { it.score }.thenBy { it.index })?.cents
        } else {
            candidates.maxByOrNull { it.cents }?.cents
        }
    }

    private fun parseAmount(raw: String): Long? {
        val normalized = if (raw.contains(',')) raw.replace(".", "").replace(',', '.') else raw
        return normalized.toBigDecimalOrNull()
            ?.setScale(2, RoundingMode.HALF_UP)
            ?.multiply(BigDecimal(100))
            ?.longValueExact()
    }

    private fun findDate(lines: List<String>): LocalDate? {
        data class DateCandidate(val date: LocalDate, val score: Int, val index: Int)
        val today = LocalDate.now()
        val candidates = mutableListOf<DateCandidate>()
        lines.forEachIndexed { index, original ->
            val normalized = normalize(original)
            dateRegex.findAll(original).forEach { match ->
                val day = match.groupValues[1].toIntOrNull() ?: return@forEach
                val month = match.groupValues[2].toIntOrNull() ?: return@forEach
                var year = match.groupValues[3].toIntOrNull() ?: return@forEach
                if (year < 100) year += 2000
                val date = runCatching { LocalDate.of(year, month, day) }.getOrNull() ?: return@forEach
                if (date.year !in 2000..today.year + 1) return@forEach
                var score = 0
                if (listOf("data", "emissao", "emitido", "compra", "transacao", "cupom").any { normalized.contains(it) }) score += 300
                if (listOf("validade", "vencimento").any { normalized.contains(it) }) score -= 300
                score -= index.coerceAtMost(100)
                candidates += DateCandidate(date, score, index)
            }
        }
        return candidates.maxWithOrNull(compareBy<DateCandidate> { it.score }.thenByDescending { it.index })?.date
    }

    private fun findMerchant(lines: List<String>): String {
        val banned = listOf(
            "cnpj", "cpf", "nota fiscal", "nfce", "nfc-e", "sat", "cupom fiscal",
            "documento auxiliar", "consumidor", "telefone", "endereco", "inscricao estadual",
            "extrato", "comprovante"
        )
        val candidate = lines.take(14).firstOrNull { original ->
            val normalized = normalize(original)
            val letters = original.count { it.isLetter() }
            letters >= 4 && banned.none { normalized.contains(it) } && !dateRegex.containsMatchIn(original)
        }.orEmpty()
        return candidate.replace(Regex("\\s+"), " ").take(48)
    }

    private fun categorize(text: String, categories: List<String>): String? {
        if (categories.isEmpty()) return null
        val all = normalize(text)
        fun preferred(name: String): String? = categories.firstOrNull { it.equals(name, ignoreCase = true) }
        fun has(vararg words: String) = words.any { all.contains(normalize(it)) }

        val standard = when {
            has("sorvete", "sorveteria", "acai", "açaí", "gelato") -> "Sorvetes"
            has("supermercado", "mercado", "hipermercado", "atacadao", "atacadão", "hortifruti", "mercearia") -> "Mercado"
            has("padaria", "panificadora", "confeitaria") -> "Padaria"
            has("posto", "gasolina", "etanol", "diesel", "combustivel", "combustível") -> "Posto de gasolina"
            has("farmacia", "farmácia", "drogaria", "medicamento") -> "Farmácia"
            has("hospital", "clinica", "clínica", "laboratorio", "laboratório", "consulta medica", "consulta médica") -> "Saúde"
            has("estacionamento", "parking") -> "Estacionamento"
            has("uber", "99app", "taxi", "táxi", "rodoviaria", "rodoviária", "passagem urbana") -> "Transporte"
            has("restaurante", "churrascaria", "pizzaria", "self service") -> "Restaurante"
            has("lanchonete", "hamburguer", "hambúrguer", "burger", "cafeteria", "lanche") -> "Lanches"
            has("cinema", "parque", "show", "ingresso", "game", "jogo") -> "Lazer"
            has("hotel", "pousada", "aeroporto", "companhia aerea", "companhia aérea", "passagem aerea", "passagem aérea") -> "Viagem"
            has("faculdade", "escola", "curso", "livraria", "mensalidade escolar") -> "Educação"
            has("roupa", "calcado", "calçado", "vestuario", "vestuário", "moda") -> "Roupas"
            has("netflix", "spotify", "assinatura", "mensalidade", "streaming") -> "Assinaturas"
            has("smartphone", "celular", "telefone movel", "telefone móvel") -> "Smartphone"
            else -> null
        }
        preferred(standard ?: "")?.let { return it }

        // User-created categories can also be recognized when their name appears clearly on the receipt.
        categories.firstOrNull { category ->
            val key = normalize(category)
            category != "Outros" && key.length >= 5 && all.contains(key)
        }?.let { return it }

        return preferred("Outros") ?: categories.first()
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
}
