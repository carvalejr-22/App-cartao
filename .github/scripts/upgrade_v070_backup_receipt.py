from pathlib import Path
import re

root = Path('.')
activity = root / 'app/src/main/java/com/carlos/appcartao/ModernCardActivity.kt'
text = activity.read_text(encoding='utf-8')

# Versioned data model + camera imports.
text = text.replace('import android.content.Context\n', 'import android.content.Context\nimport android.net.Uri\n', 1)
text = text.replace('import androidx.activity.result.IntentSenderRequest\n', '')
text = text.replace('import androidx.compose.material.icons.outlined.Edit\n', 'import androidx.compose.material.icons.outlined.Edit\nimport androidx.compose.material.icons.outlined.PhotoCamera\n', 1)
text = text.replace('import androidx.compose.runtime.LaunchedEffect\n', '')
text = text.replace('import java.math.BigDecimal\n', 'import java.io.File\nimport java.math.BigDecimal\n', 1)
text = text.replace('import javax.crypto.spec.GCMParameterSpec\n', 'import javax.crypto.spec.GCMParameterSpec\nimport androidx.core.content.FileProvider\n', 1)
text = text.replace('private const val MODERN_DATA_VERSION = 6', 'private const val MODERN_DATA_VERSION = 7', 1)
text = text.replace(
    '    val installmentNumber: Int? = null,\n    val installmentTotal: Int? = null\n',
    '    val installmentNumber: Int? = null,\n    val installmentTotal: Int? = null,\n    val isRecurring: Boolean = false\n',
    1,
)

# Replace Google Drive authorization activity plumbing with receipt camera capture.
start = text.index('class ModernCardActivity : ComponentActivity() {')
end = text.index('\n@Composable\nprivate fun ModernCreditCardApp', start)
new_activity = r'''class ModernCardActivity : ComponentActivity() {
    private lateinit var store: ModernSecureStore
    private var pendingReceiptCallback: ((ReceiptScanResult?, String?) -> Unit)? = null
    private var pendingReceiptUri: Uri? = null
    private var pendingReceiptFile: File? = null
    private var pendingReceiptCategories: List<String> = emptyList()

    private val receiptCameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
            val callback = pendingReceiptCallback
            val uri = pendingReceiptUri
            if (callback == null || uri == null) {
                clearReceiptCapture()
                return@registerForActivityResult
            }
            if (!captured) {
                callback(null, "Foto cancelada. Nenhuma compra foi alterada.")
                clearReceiptCapture()
                return@registerForActivityResult
            }

            ReceiptOcrScanner(this).scan(uri, pendingReceiptCategories) { result, error ->
                runOnUiThread {
                    callback(result, error)
                    clearReceiptCapture()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ModernSecureStore(this)
        setContent {
            MaterialTheme(colorScheme = ModernColors) {
                ModernCreditCardApp(
                    store = store,
                    onScanReceipt = ::captureAndReadReceipt
                )
            }
        }
    }

    private fun captureAndReadReceipt(
        categories: List<String>,
        callback: (ReceiptScanResult?, String?) -> Unit
    ) {
        try {
            val dir = File(cacheDir, "receipts").apply { mkdirs() }
            val file = File.createTempFile("receipt_", ".jpg", dir)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            pendingReceiptCallback = callback
            pendingReceiptCategories = categories
            pendingReceiptFile = file
            pendingReceiptUri = uri
            receiptCameraLauncher.launch(uri)
        } catch (error: Exception) {
            clearReceiptCapture()
            callback(null, "Não foi possível abrir a câmera neste aparelho.")
        }
    }

    private fun clearReceiptCapture() {
        pendingReceiptFile?.delete()
        pendingReceiptFile = null
        pendingReceiptUri = null
        pendingReceiptCategories = emptyList()
        pendingReceiptCallback = null
    }
}'''
text = text[:start] + new_activity + text[end:]

# Simplify app-level signature and make every persisted change schedule Android Backup once online.
old_sig = '''@Composable
private fun ModernCreditCardApp(
    store: ModernSecureStore,
    cloudSync: GoogleDriveSync,
    onAuthorizeAndSync: ((CloudSyncResult) -> Unit) -> Unit,
    onDisconnectCloud: ((String) -> Unit) -> Unit
) {
    var data by remember { mutableStateOf(store.read()) }
    var screen by remember { mutableStateOf(ModernScreen.HOME) }
    var cloudEnabled by remember { mutableStateOf(cloudSync.isEnabled()) }
    var cloudMessage by remember { mutableStateOf(cloudSync.lastStatusText()) }

    LaunchedEffect(Unit) {
        cloudSync.tryRestoreExistingGrant(store) { result ->
            if (result != null) {
                cloudEnabled = cloudSync.isEnabled()
                cloudMessage = result.message
                if (result.restoredFromCloud) data = store.read()
            }
        }
    }
'''
new_sig = '''@Composable
private fun ModernCreditCardApp(
    store: ModernSecureStore,
    onScanReceipt: (List<String>, (ReceiptScanResult?, String?) -> Unit) -> Unit
) {
    var data by remember { mutableStateOf(store.read()) }
    var screen by remember { mutableStateOf(ModernScreen.HOME) }
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
'''
if old_sig not in text:
    raise SystemExit('ModernCreditCardApp signature/state block not found')
text = text.replace(old_sig, new_sig, 1)
text = text.replace(
    '''        store.write(normalized)
        data = normalized
        if (cloudSync.isEnabled()) cloudSync.enqueueSync()
''',
    '''        store.write(normalized)
        data = normalized
        AutomaticBackupScheduler.schedule(appContext)
''',
    1,
)
text = text.replace(
    '''                    onSelectCard = ::selectCard,
                    onAddCard = ::addCard,
                    onAddPurchases = { purchases -> persist(data.copy(purchases = data.purchases + purchases)) }
''',
    '''                    onSelectCard = ::selectCard,
                    onAddCard = ::addCard,
                    onScanReceipt = onScanReceipt,
                    onAddPurchases = { purchases -> persist(data.copy(purchases = data.purchases + purchases)) }
''',
    1,
)

# Strip Google backup arguments from settings invocation.
cloud_args = '''                    onCategoriesChanged = { categories -> persist(data.copy(categories = categories)) },
                    cloudEnabled = cloudEnabled,
                    cloudMessage = cloudMessage,
                    onCloudSync = {
                        cloudMessage = "Conectando com a Conta Google…"
                        onAuthorizeAndSync { result ->
                            cloudEnabled = cloudSync.isEnabled()
                            cloudMessage = result.message
                            if (result.restoredFromCloud) data = store.read()
                        }
                    },
                    onCloudDisconnect = {
                        onDisconnectCloud { message ->
                            cloudEnabled = cloudSync.isEnabled()
                            cloudMessage = message
                        }
                    }
'''
if cloud_args not in text:
    raise SystemExit('Cloud settings invocation block not found')
text = text.replace(cloud_args, '                    onCategoriesChanged = { categories -> persist(data.copy(categories = categories)) }\n', 1)

# Home signature gets optional receipt scanner.
text = text.replace(
    '''private fun ModernHomeScreen(
    data: ModernAppData,
    onSelectCard: (String) -> Unit,
    onAddCard: () -> Unit,
    onAddPurchases: (List<ModernPurchase>) -> Unit
) {''',
    '''private fun ModernHomeScreen(
    data: ModernAppData,
    onSelectCard: (String) -> Unit,
    onAddCard: () -> Unit,
    onScanReceipt: (List<String>, (ReceiptScanResult?, String?) -> Unit) -> Unit,
    onAddPurchases: (List<ModernPurchase>) -> Unit
) {''',
    1,
)
text = text.replace(
    '''    var isInstallment by remember { mutableStateOf(false) }
    var installmentCount by remember { mutableStateOf("12") }
    var error by remember { mutableStateOf<String?>(null) }
''',
    '''    var isInstallment by remember { mutableStateOf(false) }
    var isRecurring by remember { mutableStateOf(false) }
    var installmentCount by remember { mutableStateOf("12") }
    var isScanningReceipt by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
''',
    1,
)

# Add receipt capture as an extra, non-invasive action above manual entry.
marker = '''        item {
            OutlinedTextField(
                value = amount,
'''
scanner_ui = '''        item {
            FilledTonalButton(
                onClick = {
                    isScanningReceipt = true
                    scanMessage = "Lendo comprovante…"
                    onScanReceipt(data.categories) { result, failure ->
                        isScanningReceipt = false
                        if (result != null) {
                            result.amountCents?.let { amount = formatModernEditableAmount(it) }
                            result.purchaseDate?.let { date = it }
                            result.category?.let { detected ->
                                data.categories.firstOrNull { it.equals(detected, ignoreCase = true) }?.let { category = it }
                            }
                            if (result.description.isNotBlank()) note = result.description
                            error = null
                            scanMessage = if (result.amountCents != null) {
                                "Comprovante lido. Confira os campos antes de salvar."
                            } else {
                                "Texto lido, mas o valor total não ficou claro. Confira e preencha o valor."
                            }
                        } else {
                            scanMessage = failure ?: "Não consegui ler esse comprovante. Tente outra foto."
                        }
                    }
                },
                enabled = !isScanningReceipt,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isScanningReceipt) "Lendo comprovante…" else "Ler comprovante pela câmera")
            }
            scanMessage?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFF667085))
            }
        }
'''
if marker not in text:
    raise SystemExit('Amount field marker not found')
text = text.replace(marker, scanner_ui + marker, 1)

# Make installment and recurring mutually exclusive and add recurring marker UI.
text = text.replace(
    '''                            onCheckedChange = {
                                isInstallment = it
                                error = null
                            }
''',
    '''                            onCheckedChange = {
                                isInstallment = it
                                if (it) isRecurring = false
                                error = null
                            }
''',
    1,
)
installment_end = '''            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
'''
recurring_ui = '''            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRecurring) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("↻", color = MaterialTheme.colorScheme.primary, fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Compra recorrente", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Marque para identificar cobranças que se repetem. Não cria lançamentos futuros sozinho.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF667085)
                        )
                    }
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = {
                            isRecurring = it
                            if (it) isInstallment = false
                        }
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
'''
if installment_end not in text:
    raise SystemExit('Installment/date boundary not found')
text = text.replace(installment_end, recurring_ui, 1)

# Persist recurring marker and reset it after saving.
text = text.replace(
    '''                                purchaseDate = date,
                                category = category,
                                note = note.trim()
''',
    '''                                purchaseDate = date,
                                category = category,
                                note = note.trim(),
                                isRecurring = isRecurring
''',
    1,
)
text = text.replace(
    '''                            isInstallment = false
                            installmentCount = "12"
                            error = null
''',
    '''                            isInstallment = false
                            isRecurring = false
                            installmentCount = "12"
                            scanMessage = null
                            error = null
''',
    1,
)

# Surface recurring marker in invoice metadata without changing layout height significantly.
text = text.replace(
    '''                            } else {
                                Text(purchase.category, style = MaterialTheme.typography.bodySmall, color = Color(0xFF667085))
                            }
''',
    '''                            } else {
                                Text(
                                    if (purchase.isRecurring) "${purchase.category} • Recorrente" else purchase.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (purchase.isRecurring) MaterialTheme.colorScheme.primary else Color(0xFF667085)
                                )
                            }
''',
    1,
)

# Replace Google backup UI with Android-managed automatic backup explanation.
old_settings_sig = '''private fun ModernSettingsScreen(
    data: ModernAppData,
    onSelectCard: (String) -> Unit,
    onSaveCard: (ModernCardProfile) -> Unit,
    onDeleteCard: (String) -> Unit,
    onCategoriesChanged: (List<String>) -> Unit,
    cloudEnabled: Boolean,
    cloudMessage: String?,
    onCloudSync: () -> Unit,
    onCloudDisconnect: () -> Unit
) {'''
new_settings_sig = '''private fun ModernSettingsScreen(
    data: ModernAppData,
    onSelectCard: (String) -> Unit,
    onSaveCard: (ModernCardProfile) -> Unit,
    onDeleteCard: (String) -> Unit,
    onCategoriesChanged: (List<String>) -> Unit
) {'''
if old_settings_sig not in text:
    raise SystemExit('Settings signature not found')
text = text.replace(old_settings_sig, new_settings_sig, 1)

backup_start = text.index('        item {\n            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {\n                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {\n                    Text("Backup na Conta Google"')
backup_end_marker = '        item {\n            Card(\n                modifier = Modifier.fillMaxWidth(),\n                shape = RoundedCornerShape(20.dp),\n                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)'
backup_end = text.index(backup_end_marker, backup_start)
new_backup_ui = '''        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Backup automático", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Ativo sem login dentro do app. Toda alteração é salva no aparelho imediatamente e entra na fila de backup quando houver internet.",
                        color = Color(0xFF667085),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("✓ Proteção automática", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                "Se estiver offline, o pedido de backup aguarda a conexão. O Android faz o envio em segundo plano no momento permitido pelo sistema.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF667085)
                            )
                        }
                    }
                    Text(
                        "Não exige Google Cloud, Client ID ou configuração do usuário dentro do aplicativo.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF98A2B3)
                    )
                }
            }
        }
'''
text = text[:backup_start] + new_backup_ui + text[backup_end:]

text = text.replace(
    '                    Text("• Funciona offline; a internet só é usada quando o backup Google está autorizado.")\n                    Text("• A sincronização usa a pasta privada appData do Google Drive, invisível no Meu Drive.")\n',
    '                    Text("• Funciona offline; alterações são salvas localmente na hora.")\n                    Text("• O pedido de backup automático só é enviado quando houver conexão.")\n',
    1,
)

# Portable recurring metadata in encrypted local store and Android backup payload.
text = text.replace(
    '                        p.installmentTotal?.let { put("installmentTotal", it) }\n',
    '                        p.installmentTotal?.let { put("installmentTotal", it) }\n                        put("isRecurring", p.isRecurring)\n',
    1,
)
text = text.replace(
    '                    installmentNumber = if (p.has("installmentNumber")) p.optInt("installmentNumber") else null,\n                    installmentTotal = if (p.has("installmentTotal")) p.optInt("installmentTotal") else null\n',
    '                    installmentNumber = if (p.has("installmentNumber")) p.optInt("installmentNumber") else null,\n                    installmentTotal = if (p.has("installmentTotal")) p.optInt("installmentTotal") else null,\n                    isRecurring = p.optBoolean("isRecurring", false)\n',
    1,
)

# Editable amount helper used by OCR prefill.
helper_marker = 'private fun parseModernMoneyToCents(raw: String): Long? {'
idx = text.index(helper_marker)
text = text[:idx] + '''private fun formatModernEditableAmount(cents: Long): String =
    "%.2f".format(Locale("pt", "BR"), cents / 100.0)

''' + text[idx:]

activity.write_text(text, encoding='utf-8')

# Remove failed OAuth/Drive implementation entirely.
gdrive = root / 'app/src/main/java/com/carlos/appcartao/GoogleDriveSync.kt'
if gdrive.exists():
    gdrive.unlink()

# Android key/value backup: local data remains AES-GCM encrypted; backup agent exports a portable snapshot
# only through Android's protected backup transport and re-encrypts with the new device Keystore on restore.
backup_file = root / 'app/src/main/java/com/carlos/appcartao/AutomaticBackup.kt'
backup_file.write_text(r'''package com.carlos.appcartao

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.BackupManager
import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters

object AutomaticBackupScheduler {
    private const val UNIQUE_WORK = "meu_cartao_android_backup_request"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<AndroidBackupRequestWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

class AndroidBackupRequestWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {
    override fun doWork(): Result {
        BackupManager(applicationContext).dataChanged()
        return Result.success()
    }
}

class ModernBackupAgent : BackupAgent() {
    companion object {
        private const val ENTITY_KEY = "modern_app_data_v1"
        private const val MAX_BACKUP_BYTES = 4_500_000
    }

    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput,
        newState: ParcelFileDescriptor?
    ) {
        val payload = ModernSecureStore(this)
            .exportPlainJson()
            .toByteArray(Charsets.UTF_8)
        if (payload.isEmpty() || payload.size > MAX_BACKUP_BYTES) return
        data.writeEntityHeader(ENTITY_KEY, payload.size)
        data.writeEntityData(payload, payload.size)
    }

    override fun onRestore(
        data: BackupDataInput,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?
    ) {
        while (data.readNextHeader()) {
            val size = data.dataSize
            if (data.key != ENTITY_KEY || size <= 0 || size > MAX_BACKUP_BYTES) {
                data.skipEntityData()
                continue
            }
            val payload = ByteArray(size)
            var offset = 0
            while (offset < size) {
                val read = data.readEntityData(payload, offset, size - offset)
                if (read <= 0) break
                offset += read
            }
            if (offset == size) {
                runCatching {
                    ModernSecureStore(this).importPlainJson(String(payload, Charsets.UTF_8))
                }
            }
        }
    }
}
''', encoding='utf-8')

# Offline bundled OCR + conservative receipt parser. Camera never auto-saves; it only pre-fills fields.
receipt_file = root / 'app/src/main/java/com/carlos/appcartao/ReceiptScanner.kt'
receipt_file.write_text(r'''package com.carlos.appcartao

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
''', encoding='utf-8')

# FileProvider path for full-resolution receipt photos.
xml_dir = root / 'app/src/main/res/xml'
xml_dir.mkdir(parents=True, exist_ok=True)
(xml_dir / 'file_paths.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>\n<paths xmlns:android="http://schemas.android.com/apk/res/android">\n    <cache-path name="receipt_images" path="receipts/" />\n</paths>\n''', encoding='utf-8')

# Manifest: Android Backup Service + FileProvider. No Google OAuth/login remains.
manifest = root / 'app/src/main/AndroidManifest.xml'
manifest_text = manifest.read_text(encoding='utf-8')
manifest_text = manifest_text.replace('        android:allowBackup="false"\n        android:fullBackupContent="false"\n', '        android:allowBackup="true"\n        android:backupAgent=".ModernBackupAgent"\n        android:fullBackupOnly="false"\n', 1)
provider = '''        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
        <meta-data
            android:name="com.google.android.backup.api_key"
            android:value="unused" />
'''
manifest_text = manifest_text.replace('        <activity\n            android:name=".ModernCardActivity"', provider + '        <activity\n            android:name=".ModernCardActivity"', 1)
manifest.write_text(manifest_text, encoding='utf-8')

# Dependencies/version.
build = root / 'app/build.gradle.kts'
build_text = build.read_text(encoding='utf-8')
build_text = build_text.replace('versionCode = 10', 'versionCode = 11', 1)
build_text = build_text.replace('versionName = "0.6.2"', 'versionName = "0.7.0"', 1)
build_text = build_text.replace('    implementation("com.google.android.gms:play-services-auth:22.0.0")\n', '')
if 'com.google.mlkit:text-recognition:16.0.1' not in build_text:
    build_text = build_text.replace('    implementation("androidx.work:work-runtime-ktx:2.11.2")\n', '    implementation("androidx.work:work-runtime-ktx:2.11.2")\n    implementation("com.google.mlkit:text-recognition:16.0.1")\n', 1)
if 'junit:junit:4.13.2' not in build_text:
    build_text = build_text.replace('\n}\n', '    testImplementation("junit:junit:4.13.2")\n}\n', 1)
build.write_text(build_text, encoding='utf-8')

# Parser tests cover the high-risk receipt extraction behavior.
test_dir = root / 'app/src/test/java/com/carlos/appcartao'
test_dir.mkdir(parents=True, exist_ok=True)
(test_dir / 'ReceiptParserTest.kt').write_text(r'''package com.carlos.appcartao

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate

class ReceiptParserTest {
    private val categories = listOf(
        "Mercado", "Padaria", "Lanches", "Sorvetes", "Posto de gasolina",
        "Farmácia", "Saúde", "Restaurante", "Assinaturas", "Outros"
    )

    @Test
    fun supermarketReceiptUsesFinalTotalAndDate() {
        val result = ReceiptParser.parse(
            """
            SUPERMERCADO BOM PRECO LTDA
            CNPJ 12.345.678/0001-90
            2 ARROZ 24,90
            1 FEIJAO 8,50
            SUBTOTAL 33,40
            DESCONTO 3,40
            VALOR TOTAL R$ 30,00
            Data: 10/09/2026 18:42
            """.trimIndent(),
            categories
        )
        assertEquals(3000L, result.amountCents)
        assertEquals(LocalDate.of(2026, 9, 10), result.purchaseDate)
        assertEquals("Mercado", result.category)
        assertEquals("SUPERMERCADO BOM PRECO LTDA", result.description)
    }

    @Test
    fun fuelReceiptPrefersPaidAmountOverChange() {
        val result = ReceiptParser.parse(
            """
            POSTO CENTRAL
            GASOLINA COMUM 5,79
            TOTAL A PAGAR 150,00
            VALOR PAGO 200,00
            TROCO 50,00
            EMISSAO 09/09/2026
            """.trimIndent(),
            categories
        )
        // When both TOTAL A PAGAR and VALOR PAGO exist, the charged total is the purchase amount.
        assertEquals(15000L, result.amountCents)
        assertEquals("Posto de gasolina", result.category)
    }

    @Test
    fun fallbackFindsLargestMoneyWhenNoTotalLabelExists() {
        val result = ReceiptParser.parse(
            """
            PADARIA DO BAIRRO
            PAO 8,50
            CAFE 6,00
            14,50
            08/09/2026
            """.trimIndent(),
            categories
        )
        assertEquals(1450L, result.amountCents)
        assertEquals("Padaria", result.category)
        assertNotNull(result.purchaseDate)
    }
}
''', encoding='utf-8')

# README accurately describes behavior and limitations.
readme = root / 'README.md'
readme_text = readme.read_text(encoding='utf-8')
readme_text = re.sub(r'## Recursos atuais — v[^\n]+', '## Recursos atuais — v0.7.0', readme_text, count=1)
readme_text = readme_text.replace('- Backup opcional na Conta Google usando a pasta privada `appData` do Google Drive.\n- Quando o backup está ativado, novas compras, edições, exclusões e mudanças de configuração entram na fila de sincronização e são enviadas quando houver internet.\n- Ao reinstalar o aplicativo ou trocar de aparelho, a mesma Conta Google pode restaurar o backup existente.\n', '- Backup automático pelo Android Backup Service, sem login ou OAuth dentro do app.\n- Toda alteração agenda um pedido de backup; se estiver offline, o WorkManager aguarda conexão antes de avisar o serviço de backup. O envio efetivo é controlado pelo Android e pode ser agrupado.\n- Registro opcional por foto de comprovante: OCR local preenche valor, data, categoria e descrição para conferência antes de salvar.\n- Marcação opcional de compra recorrente, sem gerar lançamentos futuros automaticamente.\n')
# Remove obsolete Google Drive section.
if '## Backup Google Drive' in readme_text:
    readme_text = readme_text.split('## Backup Google Drive')[0].rstrip() + '\n'
readme.write_text(readme_text, encoding='utf-8')

print('v0.7.0 upgrade prepared')
