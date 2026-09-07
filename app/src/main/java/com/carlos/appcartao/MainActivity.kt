package com.carlos.appcartao

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.KeyStore
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val categories = listOf(
    "Mercado", "Padaria", "Posto de gasolina", "Estacionamento", "Transporte",
    "Restaurante", "Lazer", "Farmácia", "Saúde", "Casa", "Assinaturas",
    "Roupas", "Educação", "Viagem", "Outros"
)

data class Purchase(
    val id: String = UUID.randomUUID().toString(),
    val amountCents: Long,
    val purchaseDate: LocalDate,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val category: String,
    val note: String = ""
)

data class CardSettings(
    val invoiceStartDay: Int = 1,
    val bestPurchaseDay: Int = 1
)

data class AppData(
    val purchases: List<Purchase> = emptyList(),
    val settings: CardSettings = CardSettings()
)

data class InvoicePeriod(val start: LocalDate, val end: LocalDate)

enum class Screen { HOME, INVOICE, ANALYSIS, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = SecureStore(this)
        setContent {
            MaterialTheme {
                CreditCardApp(store)
            }
        }
    }
}

@Composable
private fun CreditCardApp(store: SecureStore) {
    var data by remember { mutableStateOf(store.read()) }
    var screen by remember { mutableStateOf(Screen.HOME) }

    fun persist(newData: AppData) {
        store.write(newData)
        data = newData
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = screen == Screen.HOME,
                    onClick = { screen = Screen.HOME },
                    icon = { Text("+") },
                    label = { Text("Lançar") }
                )
                NavigationBarItem(
                    selected = screen == Screen.INVOICE,
                    onClick = { screen = Screen.INVOICE },
                    icon = { Text("R$") },
                    label = { Text("Fatura") }
                )
                NavigationBarItem(
                    selected = screen == Screen.ANALYSIS,
                    onClick = { screen = Screen.ANALYSIS },
                    icon = { Text("▥") },
                    label = { Text("Análises") }
                )
                NavigationBarItem(
                    selected = screen == Screen.SETTINGS,
                    onClick = { screen = Screen.SETTINGS },
                    icon = { Text("⚙") },
                    label = { Text("Ajustes") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (screen) {
                Screen.HOME -> HomeScreen(data, onAdd = { purchase ->
                    persist(data.copy(purchases = data.purchases + purchase))
                })
                Screen.INVOICE -> InvoiceScreen(data, onDelete = { id ->
                    persist(data.copy(purchases = data.purchases.filterNot { it.id == id }))
                })
                Screen.ANALYSIS -> AnalysisScreen(data)
                Screen.SETTINGS -> SettingsScreen(data.settings) { settings ->
                    persist(data.copy(settings = settings))
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(data: AppData, onAdd: (Purchase) -> Unit) {
    val today = LocalDate.now()
    val period = currentPeriod(today, data.settings.invoiceStartDay)
    val total = data.purchases
        .filter { !it.purchaseDate.isBefore(period.start) && !it.purchaseDate.isAfter(period.end) }
        .sumOf { it.amountCents }

    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(categories.first()) }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today) }
    var categoryOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Meu Cartão", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Controle offline da fatura")
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Fatura atual", style = MaterialTheme.typography.titleMedium)
                    Text(formatMoney(total), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("${formatDate(period.start)} a ${formatDate(period.end)}")
                    Spacer(Modifier.height(4.dp))
                    Text("Melhor dia para compra: ${data.settings.bestPurchaseDay}")
                }
            }
        }
        item {
            Text("Nova compra", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        item {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Valor da compra") },
                prefix = { Text("R$ ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        }
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { categoryOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Categoria: $category")
                }
                DropdownMenu(expanded = categoryOpen, onDismissRequest = { categoryOpen = false }) {
                    categories.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = { category = item; categoryOpen = false }
                        )
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descrição (opcional)") },
                singleLine = true
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { date = today }, modifier = Modifier.weight(1f)) {
                    Text(if (date == today) "Hoje ✓" else "Hoje")
                }
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                            date.year,
                            date.monthValue - 1,
                            date.dayOfMonth
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(formatDate(date))
                }
            }
        }
        if (error != null) {
            item { Text(error!!, color = MaterialTheme.colorScheme.error) }
        }
        item {
            Button(
                onClick = {
                    val cents = parseMoneyToCents(amount)
                    if (cents == null || cents <= 0) {
                        error = "Digite um valor válido. Ex.: 49,90"
                    } else {
                        onAdd(Purchase(amountCents = cents, purchaseDate = date, category = category, note = note.trim()))
                        amount = ""
                        note = ""
                        date = LocalDate.now()
                        error = null
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar compra")
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun InvoiceScreen(data: AppData, onDelete: (String) -> Unit) {
    val period = currentPeriod(LocalDate.now(), data.settings.invoiceStartDay)
    val purchases = data.purchases
        .filter { !it.purchaseDate.isBefore(period.start) && !it.purchaseDate.isAfter(period.end) }
        .sortedWith(compareByDescending<Purchase> { it.purchaseDate }.thenByDescending { it.createdAtMillis })
    val total = purchases.sumOf { it.amountCents }
    var pendingDelete by remember { mutableStateOf<Purchase?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Fatura atual", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("${formatDate(period.start)} a ${formatDate(period.end)}")
        Text(formatMoney(total), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        if (purchases.isEmpty()) {
            Text("Nenhuma compra lançada nesta fatura.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(purchases, key = { it.id }) { purchase ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(purchase.note.ifBlank { purchase.category }, fontWeight = FontWeight.SemiBold)
                                Text("${purchase.category} • ${formatDate(purchase.purchaseDate)}")
                            }
                            Text(formatMoney(purchase.amountCents), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            TextButton(onClick = { pendingDelete = purchase }) { Text("Excluir") }
                        }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }

    pendingDelete?.let { purchase ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Excluir compra?") },
            text = { Text("${purchase.note.ifBlank { purchase.category }} — ${formatMoney(purchase.amountCents)}") },
            confirmButton = {
                TextButton(onClick = { onDelete(purchase.id); pendingDelete = null }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun AnalysisScreen(data: AppData) {
    val today = LocalDate.now()
    val current = currentPeriod(today, data.settings.invoiceStartDay)
    val currentPurchases = data.purchases.filter {
        !it.purchaseDate.isBefore(current.start) && !it.purchaseDate.isAfter(current.end)
    }
    val total = currentPurchases.sumOf { it.amountCents }
    val categoryTotals = currentPurchases.groupBy { it.category }
        .mapValues { (_, values) -> values.sumOf { it.amountCents } }
        .toList()
        .sortedByDescending { it.second }

    val periods = (5 downTo 0).map { offset ->
        val reference = today.minusMonths(offset.toLong())
        currentPeriod(reference, data.settings.invoiceStartDay)
    }.distinctBy { it.start }
    val periodTotals = periods.map { p ->
        data.purchases.filter { !it.purchaseDate.isBefore(p.start) && !it.purchaseDate.isAfter(p.end) }.sumOf { it.amountCents }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Análises", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Últimas faturas", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    SimpleBarChart(periodTotals)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        periods.forEach { Text(periodLabel(it), style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Por categoria — fatura atual", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (categoryTotals.isEmpty()) {
                        Text("Ainda não há dados para analisar.")
                    } else {
                        categoryTotals.forEach { (category, cents) ->
                            val pct = if (total > 0) (cents * 100.0 / total) else 0.0
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(category)
                                Text("${formatMoney(cents)} (${String.format(Locale("pt", "BR"), "%.0f", pct)}%)")
                            }
                            Spacer(Modifier.height(5.dp))
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Resumo", style = MaterialTheme.typography.titleMedium)
                    Text("Compras na fatura: ${currentPurchases.size}")
                    Text("Total: ${formatMoney(total)}")
                    if (currentPurchases.isNotEmpty()) {
                        Text("Ticket médio: ${formatMoney(total / currentPurchases.size)}")
                        Text("Maior categoria: ${categoryTotals.firstOrNull()?.first ?: "—"}")
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun SimpleBarChart(values: List<Long>) {
    val barColor = MaterialTheme.colorScheme.primary
    val guideColor = MaterialTheme.colorScheme.outlineVariant
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        drawLine(guideColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
        if (values.isEmpty()) return@Canvas
        val slot = size.width / values.size
        val barWidth = slot * 0.58f
        values.forEachIndexed { index, value ->
            val height = size.height * (value.toFloat() / max.toFloat())
            val x = index * slot + (slot - barWidth) / 2f
            drawRect(barColor, topLeft = Offset(x, size.height - height), size = Size(barWidth, height))
        }
    }
}

@Composable
private fun SettingsScreen(settings: CardSettings, onSave: (CardSettings) -> Unit) {
    var startDay by remember(settings) { mutableStateOf(settings.invoiceStartDay.toString()) }
    var bestDay by remember(settings) { mutableStateOf(settings.bestPurchaseDay.toString()) }
    var message by remember { mutableStateOf<String?>(null) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Configurações", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            Text("Use dias de 1 a 28 para evitar problemas em meses curtos. O início define em qual fatura cada compra será agrupada.")
        }
        item {
            OutlinedTextField(
                value = startDay,
                onValueChange = { startDay = it.filter(Char::isDigit).take(2); message = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Dia de início da fatura") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = bestDay,
                onValueChange = { bestDay = it.filter(Char::isDigit).take(2); message = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Melhor dia para compra / virada") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }
        item {
            Button(
                onClick = {
                    val s = startDay.toIntOrNull()
                    val b = bestDay.toIntOrNull()
                    if (s == null || b == null || s !in 1..28 || b !in 1..28) {
                        message = "Informe dias entre 1 e 28."
                    } else {
                        onSave(CardSettings(s, b))
                        message = "Configurações salvas."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Salvar configurações") }
        }
        message?.let { msg -> item { Text(msg) } }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Privacidade e segurança", fontWeight = FontWeight.Bold)
                    Text("• O app não pede acesso à internet.")
                    Text("• Os lançamentos ficam no armazenamento privado do Android.")
                    Text("• O arquivo de dados é criptografado com uma chave protegida pelo Android Keystore.")
                    Text("• Não armazene número do cartão, CVV ou senha neste app.")
                }
            }
        }
    }
}

private fun currentPeriod(date: LocalDate, startDay: Int): InvoicePeriod {
    val safeDay = startDay.coerceIn(1, 28)
    val startThisMonth = date.withDayOfMonth(safeDay)
    val start = if (date.isBefore(startThisMonth)) startThisMonth.minusMonths(1) else startThisMonth
    return InvoicePeriod(start, start.plusMonths(1).minusDays(1))
}

private fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

private fun periodLabel(period: InvoicePeriod): String =
    period.end.format(DateTimeFormatter.ofPattern("MMM/yy", Locale("pt", "BR"))).replaceFirstChar { it.uppercase() }

private fun formatMoney(cents: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(BigDecimal(cents).divide(BigDecimal(100)))
}

private fun parseMoneyToCents(raw: String): Long? {
    val cleaned = raw.trim().replace("R$", "").replace(" ", "")
    if (cleaned.isBlank()) return null
    val normalized = if (cleaned.contains(',')) cleaned.replace(".", "").replace(',', '.') else cleaned
    return normalized.toBigDecimalOrNull()
        ?.setScale(2, RoundingMode.HALF_UP)
        ?.multiply(BigDecimal(100))
        ?.longValueExact()
}

class SecureStore(private val context: Context) {
    private val fileName = "card_data.enc"
    private val alias = "app_cartao_aes_key_v1"

    fun read(): AppData {
        val file = context.filesDir.resolve(fileName)
        if (!file.exists()) return AppData()
        return try {
            val parts = file.readText().split(':', limit = 2)
            require(parts.size == 2)
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            val json = String(cipher.doFinal(encrypted), Charsets.UTF_8)
            fromJson(JSONObject(json))
        } catch (_: Exception) {
            AppData()
        }
    }

    fun write(data: AppData) {
        val plain = toJson(data).toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plain)
        val encoded = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        val temp = context.filesDir.resolve("$fileName.tmp")
        temp.writeText(encoded)
        val target = context.filesDir.resolve(fileName)
        if (target.exists()) target.delete()
        check(temp.renameTo(target))
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(alias, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun toJson(data: AppData): JSONObject {
        val root = JSONObject()
        root.put("settings", JSONObject().apply {
            put("invoiceStartDay", data.settings.invoiceStartDay)
            put("bestPurchaseDay", data.settings.bestPurchaseDay)
        })
        root.put("purchases", JSONArray().apply {
            data.purchases.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("amountCents", p.amountCents)
                    put("purchaseEpochDay", p.purchaseDate.toEpochDay())
                    put("createdAtMillis", p.createdAtMillis)
                    put("category", p.category)
                    put("note", p.note)
                })
            }
        })
        return root
    }

    private fun fromJson(root: JSONObject): AppData {
        val settingsJson = root.optJSONObject("settings") ?: JSONObject()
        val settings = CardSettings(
            invoiceStartDay = settingsJson.optInt("invoiceStartDay", 1).coerceIn(1, 28),
            bestPurchaseDay = settingsJson.optInt("bestPurchaseDay", 1).coerceIn(1, 28)
        )
        val array = root.optJSONArray("purchases") ?: JSONArray()
        val purchases = buildList {
            for (i in 0 until array.length()) {
                val p = array.getJSONObject(i)
                add(
                    Purchase(
                        id = p.getString("id"),
                        amountCents = p.getLong("amountCents"),
                        purchaseDate = LocalDate.ofEpochDay(p.getLong("purchaseEpochDay")),
                        createdAtMillis = p.optLong("createdAtMillis", Instant.now().toEpochMilli()),
                        category = p.optString("category", "Outros"),
                        note = p.optString("note", "")
                    )
                )
            }
        }
        return AppData(purchases, settings)
    }
}
