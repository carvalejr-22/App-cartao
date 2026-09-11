package com.carlos.appcartao

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.KeyStore
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import androidx.core.content.FileProvider
import kotlin.math.max

private const val MODERN_DATA_VERSION = 8
private const val LEGACY_CARD_ID = "legacy-card-1"

private val ModernColors = lightColorScheme(
    primary = Color(0xFF176A5C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8F3EC),
    onPrimaryContainer = Color(0xFF063C34),
    secondary = Color(0xFF4B67D1),
    secondaryContainer = Color(0xFFE2E8FF),
    tertiary = Color(0xFFFF8B3D),
    background = Color(0xFFF6F7FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEF1F5),
    outlineVariant = Color(0xFFDDE2E8)
)

private val modernDefaultCategories = listOf(
    "Mercado", "Padaria", "Lanches", "Sorvetes", "Posto de gasolina", "Estacionamento", "Transporte",
    "Restaurante", "Lazer", "Farmácia", "Saúde", "Casa", "Assinaturas",
    "Roupas", "Educação", "Viagem", "Outros"
)

private val modernChartColors = listOf(
    Color(0xFF4B67D1), Color(0xFF16A36A), Color(0xFFFF8B3D), Color(0xFF8657D8),
    Color(0xFF1598B5), Color(0xFFD85868), Color(0xFFD2A51C), Color(0xFF667085)
)

private val modernFullDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val modernDayMonthFormatter = DateTimeFormatter.ofPattern("dd/MM")
private val modernInvoiceMonthFormatter = DateTimeFormatter.ofPattern("MMM/yyyy", Locale("pt", "BR"))
private val modernShortMonthFormatter = DateTimeFormatter.ofPattern("MMM", Locale("pt", "BR"))
private val modernCurrencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

data class ModernCardProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val closingDay: Int = 5,
    val dueDay: Int = 12
) {
    val bestPurchaseDay: Int get() = if (closingDay >= 31) 1 else closingDay + 1
}

data class ModernPurchase(
    val id: String = UUID.randomUUID().toString(),
    val cardId: String,
    val amountCents: Long,
    val purchaseDate: LocalDate,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val category: String,
    val note: String = "",
    val invoiceStartEpochDay: Long? = null,
    val invoiceEndEpochDay: Long? = null,
    val invoiceDueEpochDay: Long? = null,
    val installmentGroupId: String? = null,
    val installmentNumber: Int? = null,
    val installmentTotal: Int? = null,
    val isRecurring: Boolean = false,
    val recurringRuleId: String? = null
) {
    val isInstallment: Boolean
        get() = installmentGroupId != null && installmentNumber != null && installmentTotal != null && installmentTotal > 1
}

data class ModernRecurringRule(
    val id: String = UUID.randomUUID().toString(),
    val cardId: String,
    val amountCents: Long,
    val startEpochDay: Long,
    val dayOfMonth: Int,
    val category: String,
    val note: String = "",
    val active: Boolean = true,
    val skippedEpochDays: List<Long> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class ModernAppData(
    val cards: List<ModernCardProfile> = listOf(ModernCardProfile(id = LEGACY_CARD_ID, name = "Cartão 1")),
    val activeCardId: String = LEGACY_CARD_ID,
    val purchases: List<ModernPurchase> = emptyList(),
    val recurringRules: List<ModernRecurringRule> = emptyList(),
    val categories: List<String> = modernDefaultCategories,
    val lastModifiedMillis: Long = 0L
)

data class ModernInvoicePeriod(
    val start: LocalDate,
    val end: LocalDate,
    val dueDate: LocalDate
)

private enum class ModernScreen { HOME, INVOICE, ANALYSIS, SETTINGS }

class ModernCardActivity : ComponentActivity() {
    private lateinit var store: ModernSecureStore
    private var pendingReceiptCallback: ((ReceiptScanResult?, String?) -> Unit)? = null
    private var pendingReceiptUri: Uri? = null
    private var pendingReceiptFile: File? = null
    private var pendingReceiptCategories: List<String> = emptyList()

    private val receiptCameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
            val callback = pendingReceiptCallback
            val uri = pendingReceiptUri
            val file = pendingReceiptFile
            if (callback == null || uri == null || file == null) {
                clearReceiptCapture()
                return@registerForActivityResult
            }
            if (!captured) {
                callback(null, "Foto cancelada. Nenhuma compra foi alterada.")
                clearReceiptCapture()
                return@registerForActivityResult
            }

            ReceiptOcrScanner().scan(file, pendingReceiptCategories) { result, error ->
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
}
@Composable
private fun ModernCreditCardApp(
    store: ModernSecureStore,
    onScanReceipt: (List<String>, (ReceiptScanResult?, String?) -> Unit) -> Unit
) {
    var data by remember {
        val loaded = store.read()
        val prepared = modernEnsureRecurringSchedule(loaded)
        if (prepared != loaded) {
            store.write(prepared.copy(lastModifiedMillis = System.currentTimeMillis()))
        }
        mutableStateOf(prepared)
    }
    var screen by remember { mutableStateOf(ModernScreen.HOME) }
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext

    fun persist(newData: ModernAppData) {
        val normalized = modernEnsureRecurringSchedule(normalizeModernData(newData))
            .copy(lastModifiedMillis = System.currentTimeMillis())
        store.write(normalized)
        data = normalized
        AutomaticBackupScheduler.schedule(appContext)
    }

    fun selectCard(cardId: String) {
        if (data.cards.any { it.id == cardId } && data.activeCardId != cardId) {
            persist(data.copy(activeCardId = cardId))
        }
    }

    fun addCard() {
        val usedNumbers = data.cards.mapNotNull {
            Regex("^Cartão (\\d+)$", RegexOption.IGNORE_CASE).matchEntire(it.name.trim())?.groupValues?.getOrNull(1)?.toIntOrNull()
        }.toSet()
        var number = 1
        while (number in usedNumbers) number++
        val reference = data.cards.firstOrNull { it.id == data.activeCardId } ?: data.cards.first()
        val card = ModernCardProfile(
            name = "Cartão $number",
            closingDay = reference.closingDay,
            dueDay = reference.dueDay
        )
        persist(data.copy(cards = data.cards + card, activeCardId = card.id))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = screen == ModernScreen.HOME,
                    onClick = { screen = ModernScreen.HOME },
                    icon = { Text("⌂", fontSize = 24.sp) },
                    label = { Text("Início") }
                )
                NavigationBarItem(
                    selected = screen == ModernScreen.INVOICE,
                    onClick = { screen = ModernScreen.INVOICE },
                    icon = { Text("▤", fontSize = 22.sp) },
                    label = { Text("Fatura") }
                )
                NavigationBarItem(
                    selected = screen == ModernScreen.ANALYSIS,
                    onClick = { screen = ModernScreen.ANALYSIS },
                    icon = { Text("▥", fontSize = 22.sp) },
                    label = { Text("Análises") }
                )
                NavigationBarItem(
                    selected = screen == ModernScreen.SETTINGS,
                    onClick = { screen = ModernScreen.SETTINGS },
                    icon = { Text("⚙", fontSize = 22.sp) },
                    label = { Text("Ajustes") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            when (screen) {
                ModernScreen.HOME -> ModernHomeScreen(
                    data = data,
                    onSelectCard = ::selectCard,
                    onAddCard = ::addCard,
                    onScanReceipt = onScanReceipt,
                    onSaveEntry = { base, count, recurring ->
                        val targetCard = data.cards.firstOrNull { it.id == base.cardId } ?: data.cards.first()
                        val updated = when {
                            count > 1 -> data.copy(
                                purchases = data.purchases + modernBuildInstallmentSeries(base, count, targetCard)
                            )
                            recurring -> modernAddRecurringSeries(data, base, targetCard)
                            else -> data.copy(purchases = data.purchases + modernAssignInvoice(base, targetCard))
                        }
                        persist(updated)
                    }
                )

                ModernScreen.INVOICE -> ModernInvoiceScreen(
                    data = data,
                    onSelectCard = ::selectCard,
                    onDelete = { purchase ->
                        val updated = if (purchase.recurringRuleId != null) {
                            modernSkipRecurringOccurrence(data, purchase)
                        } else {
                            data.copy(purchases = data.purchases.filterNot { it.id == purchase.id })
                        }
                        persist(updated)
                    },
                    onCancelRecurring = { purchase -> persist(modernCancelRecurringFrom(data, purchase)) },
                    onEditAmount = { id, newAmount ->
                        persist(
                            data.copy(
                                purchases = data.purchases.map { purchase ->
                                    if (purchase.id == id) purchase.copy(amountCents = newAmount) else purchase
                                }
                            )
                        )
                    }
                )

                ModernScreen.ANALYSIS -> ModernAnalysisScreen(data, ::selectCard)

                ModernScreen.SETTINGS -> ModernSettingsScreen(
                    data = data,
                    onSelectCard = ::selectCard,
                    onSaveCard = { updated ->
                        persist(data.copy(cards = data.cards.map { if (it.id == updated.id) updated else it }))
                    },
                    onDeleteCard = { cardId ->
                        val mainCardId = data.cards.first().id
                        if (cardId != mainCardId && data.cards.size > 1) {
                            val remainingCards = data.cards.filterNot { it.id == cardId }
                            val nextActiveId = if (data.activeCardId == cardId) mainCardId else data.activeCardId
                            persist(
                                data.copy(
                                    cards = remainingCards,
                                    activeCardId = nextActiveId,
                                    purchases = data.purchases.filterNot { it.cardId == cardId },
                                    recurringRules = data.recurringRules.filterNot { it.cardId == cardId }
                                )
                            )
                        }
                    },
                    onCategoriesChanged = { categories -> persist(data.copy(categories = categories)) }
                )
            }
        }
    }
}

@Composable
private fun ModernHomeScreen(
    data: ModernAppData,
    onSelectCard: (String) -> Unit,
    onAddCard: () -> Unit,
    onScanReceipt: (List<String>, (ReceiptScanResult?, String?) -> Unit) -> Unit,
    onSaveEntry: (ModernPurchase, Int, Boolean) -> Unit
) {
    val today = LocalDate.now()
    val activeCard = data.cards.firstOrNull { it.id == data.activeCardId } ?: data.cards.first()
    val currentPeriod = modernInvoiceForPurchase(today, activeCard)
    val cardTotal = modernPurchasesForPeriod(data.purchases, activeCard.id, currentPeriod).sumOf { it.amountCents }
    val allCardsTotal = data.cards.sumOf { card ->
        val period = modernInvoiceForPurchase(today, card)
        modernPurchasesForPeriod(data.purchases, card.id, period).sumOf { it.amountCents }
    }

    var amount by remember { mutableStateOf("") }
    var category by remember(data.categories) { mutableStateOf(data.categories.firstOrNull() ?: "Outros") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today) }
    var showCategories by remember { mutableStateOf(false) }
    var showCardPicker by remember { mutableStateOf(false) }
    var isInstallment by remember { mutableStateOf(false) }
    var isRecurring by remember { mutableStateOf(false) }
    var installmentCount by remember { mutableStateOf("12") }
    var isScanningReceipt by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        if (data.cards.size > 1) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total das faturas atuais", color = Color.White.copy(alpha = .78f))
                            Text(
                                formatModernMoney(allCardsTotal),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        TextButton(onClick = onAddCard, modifier = Modifier.size(42.dp)) {
                            Text("+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
                        }
                    }
                }
            }
            item {
                FilledTonalButton(
                    onClick = { showCardPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(activeCard.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Trocar", style = MaterialTheme.typography.labelLarge)
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(activeCard.name, color = Color.White.copy(alpha = .78f))
                                Text(
                                    formatModernMoney(cardTotal),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            TextButton(onClick = onAddCard, modifier = Modifier.size(42.dp)) {
                                Text("+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            CompactInfoLight("Fecha", modernDayLabel(currentPeriod.end))
                            CompactInfoLight("Vence", modernDayLabel(currentPeriod.dueDate))
                            CompactInfoLight("Melhor compra", modernDayLabel(currentPeriod.end.plusDays(1)))
                        }
                    }
                }
            }
        }

        item {
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
        item {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                label = { Text(if (isInstallment) "Valor de cada parcela" else "Valor da compra") },
                prefix = { Text("R$ ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        }
        item {
            FilledTonalButton(
                onClick = { showCategories = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(modernCategorySymbol(category), fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(category, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Alterar")
            }
        }
        item {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                label = { Text("Descrição (opcional)") },
                singleLine = true
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isInstallment) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⇄", color = MaterialTheme.colorScheme.secondary, fontSize = 24.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Compra parcelada", fontWeight = FontWeight.SemiBold)
                            Text(
                                if (isInstallment) "Cada parcela entra automaticamente na fatura correta" else "Desativado: a compra será registrada à vista",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF667085)
                            )
                        }
                        Switch(
                            checked = isInstallment,
                            onCheckedChange = {
                                isInstallment = it
                                if (it) isRecurring = false
                                error = null
                            }
                        )
                    }
                    if (isInstallment) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = installmentCount,
                            onValueChange = { installmentCount = it.filter(Char::isDigit).take(2); error = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Número de parcelas") },
                            suffix = { Text("x") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            }
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
                            "Lança a cobrança mensalmente nas próximas faturas. Você pode cancelar a recorrência quando quiser.",
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
                FilledTonalButton(onClick = { date = today }, modifier = Modifier.weight(1f)) {
                    Text(if (date == today) "Hoje ✓" else "Hoje")
                }
                FilledTonalButton(
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
                ) { Text("▣  ${formatModernDate(date)}") }
            }
        }
        if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error) }
        item {
            Button(
                onClick = {
                    val cents = parseModernMoneyToCents(amount)
                    val parcels = if (isInstallment) installmentCount.toIntOrNull() else 1
                    when {
                        cents == null || cents <= 0 -> error = "Digite um valor válido. Ex.: 228,38"
                        isInstallment && (parcels == null || parcels !in 2..60) -> error = "Informe entre 2 e 60 parcelas."
                        else -> {
                            val base = ModernPurchase(
                                cardId = activeCard.id,
                                amountCents = cents,
                                purchaseDate = date,
                                category = category,
                                note = note.trim(),
                                isRecurring = isRecurring
                            )
                            val count = parcels ?: 1
                            onSaveEntry(base, count, isRecurring)
                            amount = ""
                            note = ""
                            date = LocalDate.now()
                            isInstallment = false
                            isRecurring = false
                            installmentCount = "12"
                            scanMessage = null
                            error = null
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("＋  ${if (isInstallment) "Salvar parcelas" else "Salvar compra"}", fontWeight = FontWeight.Bold)
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }

    if (showCardPicker) {
        AlertDialog(
            onDismissRequest = { showCardPicker = false },
            title = { Text("Cartão da compra") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    data.cards.forEach { card ->
                        TextButton(
                            onClick = {
                                onSelectCard(card.id)
                                showCardPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(card.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (card.id == activeCard.id) Text("✓", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCardPicker = false }) { Text("Fechar") } }
        )
    }

    if (showCategories) {
        AlertDialog(
            onDismissRequest = { showCategories = false },
            title = { Text("Categoria da compra") },
            text = {
                LazyColumn(modifier = Modifier.height(420.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(data.categories) { item ->
                        TextButton(
                            onClick = { category = item; showCategories = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(modernCategorySymbol(item), fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(item, modifier = Modifier.weight(1f))
                            if (item == category) Text("✓", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCategories = false }) { Text("Fechar") } }
        )
    }
}

@Composable
private fun CompactInfoLight(title: String, value: String) {
    Column {
        Text(title, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .70f))
        Text(value, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun CompactInfo(title: String, value: String) {
    Column {
        Text(title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF667085))
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CardSelector(
    data: ModernAppData,
    onSelectCard: (String) -> Unit
) {
    val today = LocalDate.now()
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        data.cards.forEach { card ->
            val selected = card.id == data.activeCardId
            val period = modernInvoiceForPurchase(today, card)
            val total = modernPurchasesForPeriod(data.purchases, card.id, period).sumOf { it.amountCents }
            FilledTonalButton(
                onClick = { onSelectCard(card.id) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                )
            ) {
                Text("${card.name}  ${formatModernMoney(total)}", fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun ModernInvoiceScreen(
    data: ModernAppData,
    onSelectCard: (String) -> Unit,
    onDelete: (ModernPurchase) -> Unit,
    onCancelRecurring: (ModernPurchase) -> Unit,
    onEditAmount: (String, Long) -> Unit
) {
    val card = data.cards.firstOrNull { it.id == data.activeCardId } ?: data.cards.first()
    val periods = modernAvailableInvoicePeriods(data, card)
    val current = modernInvoiceForPurchase(LocalDate.now(), card)
    var selectedEnd by remember(card.id) { mutableStateOf(current.end) }
    val selectedIndexRaw = periods.indexOfFirst { it.end == selectedEnd }
    val selectedIndex = if (selectedIndexRaw >= 0) selectedIndexRaw else periods.indexOfFirst { it.end == current.end }.coerceAtLeast(0)
    val period = periods.getOrElse(selectedIndex) { current }
    val purchases = modernPurchasesForPeriod(data.purchases, card.id, period)
        .sortedWith(compareByDescending<ModernPurchase> { it.purchaseDate }.thenByDescending { it.createdAtMillis })
    val total = purchases.sumOf { it.amountCents }
    var pendingDelete by remember { mutableStateOf<ModernPurchase?>(null) }
    var pendingEdit by remember { mutableStateOf<ModernPurchase?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Spacer(Modifier.height(6.dp)) }
        item {
            Text("Faturas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Escolha o cartão para ver os valores separadamente.", color = Color(0xFF667085))
        }
        item { CardSelector(data, onSelectCard) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(card.name, color = Color.White.copy(alpha = .78f))
                    Text(formatModernMoney(total), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(modernInvoiceMonthLabel(period), color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${modernPeriodStatus(period, current)} • vence ${formatModernDate(period.dueDate)}",
                        color = Color.White.copy(alpha = .78f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { if (selectedIndex < periods.lastIndex) selectedEnd = periods[selectedIndex + 1].end },
                    enabled = selectedIndex < periods.lastIndex
                ) { Text("‹ Anterior") }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(modernInvoiceMonthLabel(period), fontWeight = FontWeight.Bold)
                    Text(
                        "${formatModernDate(period.start)} a ${formatModernDate(period.end.minusDays(1))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF667085)
                    )
                }
                TextButton(
                    onClick = { if (selectedIndex > 0) selectedEnd = periods[selectedIndex - 1].end },
                    enabled = selectedIndex > 0
                ) { Text("Próxima ›") }
            }
        }
        if (purchases.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Text("Nenhuma compra nesta fatura.", modifier = Modifier.padding(18.dp), color = Color(0xFF667085))
                }
            }
        } else {
            items(purchases, key = { it.id }) { purchase ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text(modernCategorySymbol(purchase.category), fontSize = 20.sp) }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(purchase.note.ifBlank { purchase.category }, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (purchase.isInstallment) {
                                Text(
                                    "Parcela ${purchase.installmentNumber}/${purchase.installmentTotal} • ${purchase.category}",
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    if (purchase.isRecurring) "${purchase.category} • Recorrente" else purchase.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (purchase.isRecurring) MaterialTheme.colorScheme.primary else Color(0xFF667085)
                                )
                            }
                            Text("Compra em ${formatModernDate(purchase.purchaseDate)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF98A2B3))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(formatModernMoney(purchase.amountCents), fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clickable { pendingEdit = purchase },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Editar valor",
                                    tint = Color(0xFF7A8493),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clickable { pendingDelete = purchase },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Excluir compra",
                                    tint = Color(0xFFC62828),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    pendingEdit?.let { purchase ->
        var editAmount by remember(purchase.id) {
            mutableStateOf("%.2f".format(Locale("pt", "BR"), purchase.amountCents / 100.0))
        }
        var editError by remember(purchase.id) { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { pendingEdit = null },
            title = { Text("Editar valor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(purchase.note.ifBlank { purchase.category }, color = Color(0xFF667085))
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it; editError = null },
                        label = { Text("Valor") },
                        prefix = { Text("R$ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    editError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (purchase.isInstallment) {
                        Text(
                            "A alteração vale somente para a parcela ${purchase.installmentNumber}/${purchase.installmentTotal}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF667085)
                        )
                    } else if (purchase.recurringRuleId != null) {
                        Text(
                            "A alteração vale somente para este mês. As próximas cobranças mantêm o valor da recorrência.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF667085)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cents = parseModernMoneyToCents(editAmount)
                        if (cents == null || cents <= 0L) {
                            editError = "Digite um valor válido."
                        } else {
                            onEditAmount(purchase.id, cents)
                            pendingEdit = null
                        }
                    }
                ) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { pendingEdit = null }) { Text("Cancelar") } }
        )
    }

    pendingDelete?.let { purchase ->
        val activeRecurring = purchase.recurringRuleId?.let { ruleId ->
            data.recurringRules.any { it.id == ruleId && it.active }
        } == true
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (activeRecurring) "Compra recorrente" else if (purchase.isInstallment) "Excluir esta parcela?" else "Excluir compra?") },
            text = {
                Text(
                    when {
                        activeRecurring -> "Você pode excluir somente este lançamento ou cancelar a recorrência. Ao cancelar, este e os lançamentos futuros desta cobrança serão removidos; o histórico anterior permanece."
                        purchase.isInstallment -> "Será excluída apenas a parcela ${purchase.installmentNumber}/${purchase.installmentTotal}."
                        else -> "${purchase.note.ifBlank { purchase.category }} — ${formatModernMoney(purchase.amountCents)}"
                    }
                )
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { onDelete(purchase); pendingDelete = null }) {
                        Text(if (activeRecurring) "Excluir somente este" else "Excluir", color = Color(0xFFB42318))
                    }
                    if (activeRecurring) {
                        TextButton(onClick = { onCancelRecurring(purchase); pendingDelete = null }) {
                            Text("Cancelar recorrência e futuros", color = Color(0xFFB42318))
                        }
                    }
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Voltar") } }
        )
    }
}

@Composable
private fun ModernAnalysisScreen(data: ModernAppData, onSelectCard: (String) -> Unit) {
    val card = data.cards.firstOrNull { it.id == data.activeCardId } ?: data.cards.first()
    val current = modernInvoiceForPurchase(LocalDate.now(), card)
    val periods = remember(data.purchases, card.id, card.closingDay, card.dueDay) {
        modernAnalysisPeriods(data, card)
    }
    val purchasesByPeriod = remember(data.purchases, card.id, periods) {
        periods.map { period -> modernPurchasesForPeriod(data.purchases, card.id, period) }
    }
    val monthlyValues = remember(purchasesByPeriod) {
        purchasesByPeriod.map { values -> values.sumOf { it.amountCents } }
    }
    val currentPurchases = remember(data.purchases, card.id, current.end) {
        modernPurchasesForPeriod(data.purchases, card.id, current)
    }
    val total = currentPurchases.sumOf { it.amountCents }
    val previousPeriod = periods.dropLast(1).lastOrNull()
    val previousPurchases = remember(data.purchases, card.id, previousPeriod?.end) {
        if (previousPeriod == null) emptyList() else modernPurchasesForPeriod(data.purchases, card.id, previousPeriod)
    }

    val categoryTotals = remember(currentPurchases) {
        currentPurchases.groupBy { it.category }
            .mapValues { (_, values) -> values.sumOf { it.amountCents } }
            .toList()
            .sortedByDescending { it.second }
    }
    val crossPeriodCategoryTotals = remember(purchasesByPeriod) {
        val totals = linkedMapOf<String, Long>()
        purchasesByPeriod.flatten().forEach { purchase ->
            totals[purchase.category] = (totals[purchase.category] ?: 0L) + purchase.amountCents
        }
        totals.entries.sortedByDescending { it.value }.map { it.key to it.value }
    }
    val heatCategories = crossPeriodCategoryTotals.take(6).map { it.first }
    val heatRows = heatCategories.map { category ->
        category to purchasesByPeriod.map { purchases -> purchases.asSequence().filter { it.category == category }.sumOf { it.amountCents } }
    }
    val trendCategories = crossPeriodCategoryTotals.take(3).map { it.first }
    val categorySeries = trendCategories.map { category ->
        category to purchasesByPeriod.map { purchases -> purchases.asSequence().filter { it.category == category }.sumOf { it.amountCents } }
    }
    val currentPace = remember(currentPurchases, current.start, current.end) { modernCumulativeSeries(currentPurchases, current) }
    val previousPace = remember(previousPurchases, previousPeriod?.start, previousPeriod?.end) {
        previousPeriod?.let { modernCumulativeSeries(previousPurchases, it) } ?: emptyList()
    }
    val biggestPurchase = currentPurchases.maxByOrNull { it.amountCents }
    val previousTotal = previousPurchases.sumOf { it.amountCents }
    val changeText = when {
        previousTotal <= 0L -> "Sem base anterior"
        else -> {
            val change = ((total - previousTotal).toDouble() / previousTotal.toDouble()) * 100.0
            if (change >= 0) "+%.0f%% vs. anterior".format(Locale("pt", "BR"), change)
            else "%.0f%% vs. anterior".format(Locale("pt", "BR"), change)
        }
    }
    val topShare = categoryTotals.firstOrNull()?.let { (_, cents) ->
        if (total > 0L) (cents * 100L / total).toInt() else 0
    } ?: 0
    var showTrend by remember { mutableStateOf(true) }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Spacer(Modifier.height(6.dp)) }
        item {
            Text("Análises", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Leitura visual do comportamento de compra.", color = Color(0xFF667085))
        }
        item { CardSelector(data, onSelectCard) }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Fatura atual • ${card.name}", color = Color(0xFF667085))
                    Text(formatModernMoney(total), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (currentPurchases.isEmpty()) "Sem lançamentos nesta fatura" else "${currentPurchases.size} lançamentos • ticket médio ${formatModernMoney(total / currentPurchases.size)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InsightPill("Variação", changeText, Modifier.weight(1f))
                        InsightPill("Maior categoria", if (categoryTotals.isEmpty()) "—" else "$topShare%", Modifier.weight(1f))
                    }
                    biggestPurchase?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Maior compra: ${it.note.ifBlank { it.category }} • ${formatModernMoney(it.amountCents)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF667085)
                        )
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Trajetória das faturas", fontWeight = FontWeight.Bold)
                            Text("Linha real + tendência das últimas 6 faturas", style = MaterialTheme.typography.bodySmall, color = Color(0xFF667085))
                        }
                        Text("Tendência", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(6.dp))
                        Switch(checked = showTrend, onCheckedChange = { showTrend = it })
                    }
                    Spacer(Modifier.height(12.dp))
                    ModernTrendLineChart(monthlyValues, showTrend)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        periods.forEachIndexed { index, period ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(modernShortPeriodLabel(period), style = MaterialTheme.typography.labelSmall)
                                Text(formatModernCompactMoney(monthlyValues.getOrElse(index) { 0L }), style = MaterialTheme.typography.labelSmall, color = Color(0xFF667085))
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Mapa de intensidade", fontWeight = FontWeight.Bold)
                    Text("Onde cada categoria pesou mais em cada fatura", style = MaterialTheme.typography.bodySmall, color = Color(0xFF667085))
                    Spacer(Modifier.height(14.dp))
                    if (heatRows.isEmpty()) {
                        Text("Ainda não há histórico suficiente.", color = Color(0xFF667085))
                    } else {
                        ModernCategoryHeatmap(periods, heatRows)
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Ritmo da fatura", fontWeight = FontWeight.Bold)
                    Text("Acúmulo de gastos no ciclo atual versus o anterior", style = MaterialTheme.typography.bodySmall, color = Color(0xFF667085))
                    Spacer(Modifier.height(12.dp))
                    if (currentPace.all { it == 0L } && previousPace.all { it == 0L }) {
                        Text("Sem dados para comparar.", color = Color(0xFF667085))
                    } else {
                        ModernPaceChart(currentPace, previousPace)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            ChartLegendDot("Atual", MaterialTheme.colorScheme.primary)
                            ChartLegendDot("Anterior", MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Categorias em movimento", fontWeight = FontWeight.Bold)
                    Text("Tendência das 3 categorias com maior peso recente", style = MaterialTheme.typography.bodySmall, color = Color(0xFF667085))
                    Spacer(Modifier.height(12.dp))
                    if (categorySeries.isEmpty()) {
                        Text("Sem dados para analisar.", color = Color(0xFF667085))
                    } else {
                        ModernCategoryTrendChart(categorySeries)
                        Spacer(Modifier.height(8.dp))
                        categorySeries.forEachIndexed { index, (category, _) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(9.dp).background(modernChartColors[index % modernChartColors.size], CircleShape))
                                Spacer(Modifier.width(7.dp))
                                Text("${modernCategorySymbol(category)} $category", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Detalhe por categoria • fatura atual", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    if (categoryTotals.isEmpty()) {
                        Text("Sem dados para analisar.", color = Color(0xFF667085))
                    } else {
                        val visibleCategories = categoryTotals.take(8)
                        visibleCategories.forEachIndexed { index, (category, cents) ->
                            val share = if (total > 0L) cents.toDouble() / total.toDouble() else 0.0
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(modernChartColors[index % modernChartColors.size], CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${modernCategorySymbol(category)} $category", fontWeight = FontWeight.Medium)
                                    Text("${(share * 100).toInt()}% da fatura", style = MaterialTheme.typography.labelSmall, color = Color(0xFF667085))
                                }
                                Text(formatModernMoney(cents), fontWeight = FontWeight.SemiBold)
                            }
                            if (index < visibleCategories.lastIndex) {
                                Spacer(Modifier.height(7.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(Modifier.height(7.dp))
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun InsightPill(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF667085))
            Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ChartLegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(9.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF667085))
    }
}

@Composable
private fun ModernTrendLineChart(values: List<Long>, showTrend: Boolean) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    val actual = MaterialTheme.colorScheme.secondary
    val trend = MaterialTheme.colorScheme.primary
    val maxValue = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Canvas(modifier = Modifier.fillMaxWidth().height(170.dp)) {
        repeat(4) { i ->
            val y = size.height * i / 3f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
        }
        if (values.isEmpty()) return@Canvas
        val step = if (values.size <= 1) size.width else size.width / (values.size - 1)
        fun point(index: Int, value: Long): Offset {
            val y = size.height - size.height * .88f * value.toFloat() / maxValue.toFloat() - size.height * .06f
            return Offset(if (values.size <= 1) size.width / 2f else index * step, y)
        }
        values.zipWithNext().forEachIndexed { index, pair ->
            drawLine(actual, point(index, pair.first), point(index + 1, pair.second), strokeWidth = 5f)
        }
        values.forEachIndexed { index, value -> drawCircle(actual, radius = 6f, center = point(index, value)) }

        if (showTrend && values.size >= 2) {
            val n = values.size.toDouble()
            val sumX = values.indices.sumOf { it.toDouble() }
            val sumY = values.sumOf { it.toDouble() }
            val sumXY = values.indices.sumOf { it.toDouble() * values[it].toDouble() }
            val sumXX = values.indices.sumOf { it.toDouble() * it.toDouble() }
            val denominator = n * sumXX - sumX * sumX
            val slope = if (denominator == 0.0) 0.0 else (n * sumXY - sumX * sumY) / denominator
            val intercept = (sumY - slope * sumX) / n
            fun trendPoint(index: Int): Offset {
                val predicted = max(0.0, intercept + slope * index).toLong()
                return point(index, predicted)
            }
            drawLine(trend, trendPoint(0), trendPoint(values.lastIndex), strokeWidth = 3f)
        }
    }
}

@Composable
private fun ModernCategoryHeatmap(
    periods: List<ModernInvoicePeriod>,
    rows: List<Pair<String, List<Long>>>
) {
    val maxValue = rows.flatMap { it.second }.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(104.dp))
            periods.forEach { period ->
                Text(
                    modernShortPeriodLabel(period),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF667085)
                )
            }
        }
        rows.forEach { (category, values) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${modernCategorySymbol(category)} ${category.take(11)}",
                    modifier = Modifier.width(104.dp),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                values.forEach { cents ->
                    val intensity = cents.toFloat() / maxValue.toFloat()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .height(25.dp)
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(alpha = .10f + .82f * intensity),
                                RoundedCornerShape(6.dp)
                            )
                    )
                }
            }
        }
        Text("Mais intenso = maior gasto relativo no período mostrado.", style = MaterialTheme.typography.labelSmall, color = Color(0xFF98A2B3))
    }
}

@Composable
private fun ModernPaceChart(current: List<Long>, previous: List<Long>) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    val currentColor = MaterialTheme.colorScheme.primary
    val previousColor = MaterialTheme.colorScheme.tertiary
    val maxValue = (current + previous).maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val pointCount = max(current.size, previous.size).coerceAtLeast(1)
    Canvas(modifier = Modifier.fillMaxWidth().height(170.dp)) {
        repeat(4) { i ->
            val y = size.height * i / 3f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
        }
        fun drawSeries(values: List<Long>, color: Color) {
            if (values.isEmpty()) return
            val step = if (pointCount <= 1) size.width else size.width / (pointCount - 1)
            fun p(index: Int, value: Long): Offset {
                val y = size.height - size.height * .88f * value.toFloat() / maxValue.toFloat() - size.height * .06f
                return Offset(if (pointCount <= 1) size.width / 2f else index * step, y)
            }
            values.zipWithNext().forEachIndexed { index, pair ->
                drawLine(color, p(index, pair.first), p(index + 1, pair.second), strokeWidth = 4f)
            }
            drawCircle(color, radius = 5f, center = p(values.lastIndex, values.last()))
        }
        drawSeries(previous, previousColor)
        drawSeries(current, currentColor)
    }
}

@Composable
private fun ModernCategoryTrendChart(series: List<Pair<String, List<Long>>>) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    val maxValue = series.flatMap { it.second }.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val count = series.maxOfOrNull { it.second.size } ?: 0
    Canvas(modifier = Modifier.fillMaxWidth().height(175.dp)) {
        repeat(4) { i ->
            val y = size.height * i / 3f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
        }
        if (count == 0) return@Canvas
        val step = if (count <= 1) size.width else size.width / (count - 1)
        series.forEachIndexed { seriesIndex, (_, values) ->
            val color = modernChartColors[seriesIndex % modernChartColors.size]
            fun p(index: Int, value: Long): Offset {
                val y = size.height - size.height * .88f * value.toFloat() / maxValue.toFloat() - size.height * .06f
                return Offset(if (count <= 1) size.width / 2f else index * step, y)
            }
            values.zipWithNext().forEachIndexed { index, pair ->
                drawLine(color, p(index, pair.first), p(index + 1, pair.second), strokeWidth = 4f)
            }
            values.forEachIndexed { index, value -> drawCircle(color, radius = 4.5f, center = p(index, value)) }
        }
    }
}

private fun modernCumulativeSeries(
    purchases: List<ModernPurchase>,
    period: ModernInvoicePeriod,
    points: Int = 12
): List<Long> {
    val days = (period.end.toEpochDay() - period.start.toEpochDay()).coerceAtLeast(1L)
    return (1..points).map { step ->
        val cutoffDays = (days * step / points).coerceAtLeast(1L)
        val cutoff = period.start.plusDays(cutoffDays)
        purchases.asSequence()
            .filter { it.purchaseDate.isBefore(cutoff) }
            .sumOf { it.amountCents }
    }
}

private fun formatModernCompactMoney(cents: Long): String {
    val value = cents / 100.0
    return when {
        value >= 1_000_000 -> "R$ %.1fM".format(Locale("pt", "BR"), value / 1_000_000.0)
        value >= 1_000 -> "R$ %.1fk".format(Locale("pt", "BR"), value / 1_000.0)
        else -> "R$ %.0f".format(Locale("pt", "BR"), value)
    }
}

@Composable
private fun ModernSettingsScreen(
    data: ModernAppData,
    onSelectCard: (String) -> Unit,
    onSaveCard: (ModernCardProfile) -> Unit,
    onDeleteCard: (String) -> Unit,
    onCategoriesChanged: (List<String>) -> Unit
) {
    val card = data.cards.firstOrNull { it.id == data.activeCardId } ?: data.cards.first()
    var name by remember(card.id, card.name) { mutableStateOf(card.name) }
    var closingDay by remember(card.id, card.closingDay) { mutableStateOf(card.closingDay.toString()) }
    var dueDay by remember(card.id, card.dueDay) { mutableStateOf(card.dueDay.toString()) }
    var message by remember(card.id) { mutableStateOf<String?>(null) }
    var newCategory by remember { mutableStateOf("") }
    var confirmDeleteCard by remember(card.id) { mutableStateOf(false) }
    val current = modernInvoiceForPurchase(LocalDate.now(), card)
    val isMainCard = card.id == data.cards.first().id
    val backupContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val backupStatus = remember(data.lastModifiedMillis) { AutomaticBackupStatusStore.read(backupContext) }
    val backupStatusText = when {
        backupStatus.lastRestoreMillis > 0L && backupStatus.lastRestoreMillis >= backupStatus.lastCompletedMillis ->
            "Dados restaurados do backup em ${formatAutomaticBackupMoment(backupStatus.lastRestoreMillis)}."
        backupStatus.pending && backupStatus.lastRequestMillis >= backupStatus.lastChangeMillis && backupStatus.lastRequestMillis > 0L ->
            "Alteração salva. Pedido enviado ao Android; aguardando a conclusão do backup."
        backupStatus.pending ->
            "Alteração salva no aparelho. Backup aguardando conexão com a internet."
        backupStatus.lastCompletedMillis > 0L ->
            "Último backup confirmado pelo Android: ${formatAutomaticBackupMoment(backupStatus.lastCompletedMillis)}."
        else ->
            "Ainda não há backup confirmado neste aparelho. Faça uma alteração e mantenha o celular conectado à internet."
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Spacer(Modifier.height(6.dp)) }
        item {
            Text("Ajustes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Configurações independentes para cada cartão.", color = Color(0xFF667085))
        }
        item { CardSelector(data, onSelectCard) }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Configurar ${card.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(28); message = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nome do cartão") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = closingDay,
                        onValueChange = { closingDay = it.filter(Char::isDigit).take(2); message = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Dia do fechamento") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dueDay,
                        onValueChange = { dueDay = it.filter(Char::isDigit).take(2); message = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Dia do vencimento") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Text("Ciclo atual", fontWeight = FontWeight.Bold)
                            Text("Compras ${formatModernDate(current.start)} a ${formatModernDate(current.end.minusDays(1))}")
                            Text("Fecha ${formatModernDate(current.end)} • vence ${formatModernDate(current.dueDate)}")
                            Text("Melhor compra: ${modernDayLabel(current.end.plusDays(1))}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Button(
                        onClick = {
                            val c = closingDay.toIntOrNull()
                            val d = dueDay.toIntOrNull()
                            val cleanName = name.trim()
                            when {
                                cleanName.isBlank() -> message = "Informe um nome para o cartão."
                                c == null || d == null || c !in 1..31 || d !in 1..31 -> message = "Informe dias entre 1 e 31."
                                else -> {
                                    onSaveCard(card.copy(name = cleanName, closingDay = c, dueDay = d))
                                    message = "Cartão salvo. O histórico anterior foi preservado."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Salvar cartão", fontWeight = FontWeight.Bold) }
                    if (!isMainCard && data.cards.size > 1) {
                        OutlinedButton(
                            onClick = { confirmDeleteCard = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Remover cartão", color = Color(0xFFB42318))
                        }
                    }
                    message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Categorias", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("As categorias são compartilhadas entre todos os cartões.", color = Color(0xFF667085), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newCategory,
                            onValueChange = { newCategory = it.take(28) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Nova categoria") },
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val cleaned = newCategory.trim()
                                if (cleaned.isNotEmpty() && data.categories.none { it.equals(cleaned, ignoreCase = true) }) {
                                    onCategoriesChanged((data.categories + cleaned).distinct())
                                    newCategory = ""
                                }
                            },
                            modifier = Modifier.height(54.dp)
                        ) { Text("+") }
                    }
                    Spacer(Modifier.height(8.dp))
                    data.categories.forEachIndexed { index, category ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(modernCategorySymbol(category), fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(category, modifier = Modifier.weight(1f))
                            if (category != "Outros") {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clickable {
                                            val updated = data.categories.filterNot { it == category }
                                            onCategoriesChanged(if (updated.isEmpty()) listOf("Outros") else updated)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "Excluir categoria",
                                        tint = Color(0xFFC62828),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                        if (index < data.categories.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
        item {
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
                                backupStatusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF667085)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "O Android decide o instante exato do envio. Só desinstale para testar quando aparecer ‘Último backup confirmado’.",
                                style = MaterialTheme.typography.labelSmall,
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
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Leve e privado", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("• Funciona offline; alterações são salvas localmente na hora.")
                    Text("• O pedido de backup automático só é enviado quando houver conexão.")
                    Text("• Dados criptografados com AES-GCM e chave no Android Keystore.")
                    Text("• Interface usa listas sob demanda para evitar carregar itens fora da tela.")
                    Text("• Evite salvar número do cartão, CVV ou senha nas descrições.")
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    if (confirmDeleteCard && !isMainCard) {
        AlertDialog(
            onDismissRequest = { confirmDeleteCard = false },
            title = { Text("Remover ${card.name}?") },
            text = { Text("O cartão e os lançamentos vinculados a ele serão removidos deste dispositivo. O cartão principal será mantido.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeleteCard = false
                        onDeleteCard(card.id)
                    }
                ) { Text("Remover", color = Color(0xFFB42318)) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteCard = false }) { Text("Cancelar") } }
        )
    }
}

private fun normalizeModernData(data: ModernAppData): ModernAppData {
    val cards = data.cards.mapIndexed { index, card ->
        card.copy(
            name = card.name.trim().ifBlank { "Cartão ${index + 1}" },
            closingDay = card.closingDay.coerceIn(1, 31),
            dueDay = card.dueDay.coerceIn(1, 31)
        )
    }.distinctBy { it.id }.ifEmpty { listOf(ModernCardProfile(id = LEGACY_CARD_ID, name = "Cartão 1")) }
    val active = if (cards.any { it.id == data.activeCardId }) data.activeCardId else cards.first().id
    val categories = data.categories.map { it.trim() }.filter { it.isNotEmpty() }.distinct().toMutableList()
    if (categories.none { it == "Outros" }) categories.add("Outros")
    val validIds = cards.map { it.id }.toSet()
    val purchases = data.purchases.map { purchase ->
        if (purchase.cardId in validIds) purchase else purchase.copy(cardId = cards.first().id)
    }
    return data.copy(cards = cards, activeCardId = active, purchases = purchases, categories = categories)
}

private fun modernAssignInvoice(purchase: ModernPurchase, card: ModernCardProfile): ModernPurchase {
    if (purchase.invoiceStartEpochDay != null && purchase.invoiceEndEpochDay != null && purchase.invoiceDueEpochDay != null) return purchase
    return modernAssignToPeriod(purchase, modernInvoiceForPurchase(purchase.purchaseDate, card))
}

private fun modernAssignToPeriod(purchase: ModernPurchase, period: ModernInvoicePeriod): ModernPurchase = purchase.copy(
    invoiceStartEpochDay = period.start.toEpochDay(),
    invoiceEndEpochDay = period.end.toEpochDay(),
    invoiceDueEpochDay = period.dueDate.toEpochDay()
)

private fun modernBuildInstallmentSeries(base: ModernPurchase, count: Int, card: ModernCardProfile): List<ModernPurchase> {
    val groupId = UUID.randomUUID().toString()
    val result = ArrayList<ModernPurchase>(count)
    var period = modernInvoiceForPurchase(base.purchaseDate, card)
    repeat(count) { index ->
        val installment = base.copy(
            id = UUID.randomUUID().toString(),
            createdAtMillis = base.createdAtMillis + index,
            installmentGroupId = groupId,
            installmentNumber = index + 1,
            installmentTotal = count,
            invoiceStartEpochDay = null,
            invoiceEndEpochDay = null,
            invoiceDueEpochDay = null
        )
        result.add(modernAssignToPeriod(installment, period))
        period = modernNextInvoicePeriod(period, card)
    }
    return result
}

private fun modernAddRecurringSeries(
    data: ModernAppData,
    base: ModernPurchase,
    card: ModernCardProfile
): ModernAppData {
    val ruleId = UUID.randomUUID().toString()
    val rule = ModernRecurringRule(
        id = ruleId,
        cardId = card.id,
        amountCents = base.amountCents,
        startEpochDay = base.purchaseDate.toEpochDay(),
        dayOfMonth = base.purchaseDate.dayOfMonth,
        category = base.category,
        note = base.note,
        createdAtMillis = base.createdAtMillis
    )
    val first = modernAssignInvoice(
        base.copy(isRecurring = true, recurringRuleId = ruleId),
        card
    )
    return modernEnsureRecurringSchedule(
        data.copy(
            purchases = data.purchases + first,
            recurringRules = data.recurringRules + rule
        )
    )
}

private fun modernMigrateLegacyRecurring(data: ModernAppData): ModernAppData {
    if (data.purchases.none { it.isRecurring && it.recurringRuleId == null }) return data
    val rules = data.recurringRules.toMutableList()
    val purchases = data.purchases.map { purchase ->
        if (!purchase.isRecurring || purchase.recurringRuleId != null) {
            purchase
        } else {
            val ruleId = UUID.randomUUID().toString()
            rules += ModernRecurringRule(
                id = ruleId,
                cardId = purchase.cardId,
                amountCents = purchase.amountCents,
                startEpochDay = purchase.purchaseDate.toEpochDay(),
                dayOfMonth = purchase.purchaseDate.dayOfMonth,
                category = purchase.category,
                note = purchase.note,
                createdAtMillis = purchase.createdAtMillis
            )
            purchase.copy(recurringRuleId = ruleId)
        }
    }
    return data.copy(purchases = purchases, recurringRules = rules)
}

private fun modernEnsureRecurringSchedule(input: ModernAppData): ModernAppData {
    val data = modernMigrateLegacyRecurring(input)
    if (data.recurringRules.none { it.active }) return data
    val cardById = data.cards.associateBy { it.id }
    val purchases = data.purchases.toMutableList()
    val existing = purchases.mapNotNull { purchase ->
        purchase.recurringRuleId?.let { ruleId -> "$ruleId:${purchase.purchaseDate.toEpochDay()}" }
    }.toMutableSet()
    val horizon = YearMonth.from(LocalDate.now().plusMonths(12))

    data.recurringRules.asSequence().filter { it.active }.forEach { rule ->
        val card = cardById[rule.cardId] ?: return@forEach
        val start = LocalDate.ofEpochDay(rule.startEpochDay)
        var month = YearMonth.from(start)
        while (!month.isAfter(horizon)) {
            val occurrenceDate = modernDateAtDay(month.year, month.monthValue, rule.dayOfMonth)
            val key = "${rule.id}:${occurrenceDate.toEpochDay()}"
            if (
                !occurrenceDate.isBefore(start) &&
                occurrenceDate.toEpochDay() !in rule.skippedEpochDays &&
                key !in existing
            ) {
                val purchase = ModernPurchase(
                    cardId = rule.cardId,
                    amountCents = rule.amountCents,
                    purchaseDate = occurrenceDate,
                    createdAtMillis = rule.createdAtMillis + purchases.size,
                    category = rule.category,
                    note = rule.note,
                    isRecurring = true,
                    recurringRuleId = rule.id
                )
                purchases += modernAssignInvoice(purchase, card)
                existing += key
            }
            month = month.plusMonths(1)
        }
    }
    return if (purchases == data.purchases) data else data.copy(purchases = purchases)
}

private fun modernSkipRecurringOccurrence(data: ModernAppData, purchase: ModernPurchase): ModernAppData {
    val ruleId = purchase.recurringRuleId ?: return data.copy(
        purchases = data.purchases.filterNot { it.id == purchase.id }
    )
    val epoch = purchase.purchaseDate.toEpochDay()
    val rules = data.recurringRules.map { rule ->
        if (rule.id == ruleId) rule.copy(skippedEpochDays = (rule.skippedEpochDays + epoch).distinct()) else rule
    }
    return data.copy(
        recurringRules = rules,
        purchases = data.purchases.filterNot { it.id == purchase.id }
    )
}

private fun modernCancelRecurringFrom(data: ModernAppData, purchase: ModernPurchase): ModernAppData {
    val ruleId = purchase.recurringRuleId ?: return data.copy(
        purchases = data.purchases.filterNot { it.id == purchase.id }
    )
    val fromDate = purchase.purchaseDate
    return data.copy(
        recurringRules = data.recurringRules.map { rule ->
            if (rule.id == ruleId) rule.copy(active = false) else rule
        },
        purchases = data.purchases.filterNot { candidate ->
            candidate.recurringRuleId == ruleId && !candidate.purchaseDate.isBefore(fromDate)
        }
    )
}

private fun modernInstallmentPeriod(purchaseDate: LocalDate, installmentNumber: Int, card: ModernCardProfile): ModernInvoicePeriod {
    var period = modernInvoiceForPurchase(purchaseDate, card)
    repeat((installmentNumber - 1).coerceAtLeast(0)) { period = modernNextInvoicePeriod(period, card) }
    return period
}

private fun modernNextInvoicePeriod(period: ModernInvoicePeriod, card: ModernCardProfile): ModernInvoicePeriod {
    val nextMonth = period.end.plusMonths(1)
    val end = modernDateAtDay(nextMonth.year, nextMonth.monthValue, card.closingDay)
    val previousMonth = end.minusMonths(1)
    val start = modernDateAtDay(previousMonth.year, previousMonth.monthValue, card.closingDay)
    val dueBase = if (card.dueDay > card.closingDay) end else end.plusMonths(1)
    val dueDate = modernDateAtDay(dueBase.year, dueBase.monthValue, card.dueDay)
    return ModernInvoicePeriod(start, end, dueDate)
}

private fun modernInvoiceForPurchase(date: LocalDate, card: ModernCardProfile): ModernInvoicePeriod {
    val closeThisMonth = modernDateAtDay(date.year, date.monthValue, card.closingDay)
    val end = if (!date.isBefore(closeThisMonth)) {
        val next = date.plusMonths(1)
        modernDateAtDay(next.year, next.monthValue, card.closingDay)
    } else {
        closeThisMonth
    }
    val previousMonth = end.minusMonths(1)
    val start = modernDateAtDay(previousMonth.year, previousMonth.monthValue, card.closingDay)
    val dueBase = if (card.dueDay > card.closingDay) end else end.plusMonths(1)
    val dueDate = modernDateAtDay(dueBase.year, dueBase.monthValue, card.dueDay)
    return ModernInvoicePeriod(start, end, dueDate)
}

private fun modernPurchasePeriod(purchase: ModernPurchase, card: ModernCardProfile): ModernInvoicePeriod {
    val start = purchase.invoiceStartEpochDay
    val end = purchase.invoiceEndEpochDay
    val due = purchase.invoiceDueEpochDay
    return if (start != null && end != null && due != null) {
        ModernInvoicePeriod(LocalDate.ofEpochDay(start), LocalDate.ofEpochDay(end), LocalDate.ofEpochDay(due))
    } else {
        modernInvoiceForPurchase(purchase.purchaseDate, card)
    }
}

private fun modernPurchasesForPeriod(
    purchases: List<ModernPurchase>,
    cardId: String,
    period: ModernInvoicePeriod
): List<ModernPurchase> = purchases.filter { it.cardId == cardId && it.invoiceEndEpochDay == period.end.toEpochDay() }

private fun modernAvailableInvoicePeriods(data: ModernAppData, card: ModernCardProfile): List<ModernInvoicePeriod> {
    val current = modernInvoiceForPurchase(LocalDate.now(), card)
    val fromPurchases = data.purchases.asSequence()
        .filter { it.cardId == card.id }
        .map { modernPurchasePeriod(it, card) }
        .toList()
    val previous = modernInvoiceForPurchase(current.start.minusDays(1), card)
    return (listOf(current, previous) + fromPurchases).distinctBy { it.end }.sortedByDescending { it.end }
}

private fun modernAnalysisPeriods(data: ModernAppData, card: ModernCardProfile): List<ModernInvoicePeriod> {
    val current = modernInvoiceForPurchase(LocalDate.now(), card)
    val available = modernAvailableInvoicePeriods(data, card).filter { !it.end.isAfter(current.end) }.toMutableList()
    var cursor = current
    if (available.none { it.end == cursor.end }) available.add(cursor)
    while (available.size < 6) {
        cursor = modernInvoiceForPurchase(cursor.start.minusDays(1), card)
        if (available.none { it.end == cursor.end }) available.add(cursor)
    }
    return available.sortedBy { it.end }.takeLast(6)
}

private fun modernPeriodStatus(period: ModernInvoicePeriod, current: ModernInvoicePeriod): String = when {
    period.end.isAfter(current.end) -> "Fatura futura"
    period.end.isBefore(current.end) -> "Fatura fechada"
    else -> "Fatura atual"
}

private fun modernDateAtDay(year: Int, month: Int, requestedDay: Int): LocalDate {
    val ym = YearMonth.of(year, month)
    return LocalDate.of(year, month, requestedDay.coerceIn(1, ym.lengthOfMonth()))
}

private fun modernCategorySymbol(category: String): String = when (category) {
    "Mercado" -> "🛒"
    "Padaria" -> "🥖"
    "Lanches" -> "🍔"
    "Sorvetes" -> "🍦"
    "Posto de gasolina" -> "⛽"
    "Estacionamento" -> "🅿"
    "Transporte" -> "🚌"
    "Restaurante" -> "🍽"
    "Lazer" -> "🎮"
    "Farmácia" -> "💊"
    "Saúde" -> "❤"
    "Casa" -> "⌂"
    "Assinaturas" -> "↻"
    "Roupas" -> "👕"
    "Educação" -> "🎓"
    "Viagem" -> "✈"
    else -> "•"
}

private fun formatAutomaticBackupMoment(millis: Long): String =
    java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(java.util.Date(millis))

private fun formatModernDate(date: LocalDate): String = date.format(modernFullDateFormatter)
private fun modernDayLabel(date: LocalDate): String = date.format(modernDayMonthFormatter)
private fun modernInvoiceMonthLabel(period: ModernInvoicePeriod): String =
    period.dueDate.format(modernInvoiceMonthFormatter).replaceFirstChar { it.uppercase() }
private fun modernShortPeriodLabel(period: ModernInvoicePeriod): String =
    period.dueDate.format(modernShortMonthFormatter).replaceFirstChar { it.uppercase() }

private fun formatModernMoney(cents: Long): String =
    modernCurrencyFormatter.format(BigDecimal(cents).divide(BigDecimal(100)))

private fun formatModernEditableAmount(cents: Long): String =
    "%.2f".format(Locale("pt", "BR"), cents / 100.0)

private fun parseModernMoneyToCents(raw: String): Long? {
    val cleaned = raw.trim().replace("R$", "").replace(" ", "")
    if (cleaned.isBlank()) return null
    val normalized = if (cleaned.contains(',')) cleaned.replace(".", "").replace(',', '.') else cleaned
    return normalized.toBigDecimalOrNull()
        ?.setScale(2, RoundingMode.HALF_UP)
        ?.multiply(BigDecimal(100))
        ?.longValueExact()
}

class ModernSecureStore(private val context: Context) {
    private val fileName = "card_data.enc"
    private val alias = "app_cartao_aes_key_v1"

    fun read(): ModernAppData {
        val file = context.filesDir.resolve(fileName)
        if (!file.exists()) return ModernAppData()
        return try {
            val parts = file.readText().split(':', limit = 2)
            require(parts.size == 2)
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            val json = String(cipher.doFinal(encrypted), Charsets.UTF_8)
            val decoded = normalizeModernData(fromJson(JSONObject(json)))
            if (decoded.lastModifiedMillis > 0L) decoded
            else decoded.copy(lastModifiedMillis = file.lastModified().coerceAtLeast(1L))
        } catch (_: Exception) {
            ModernAppData()
        }
    }

    fun write(data: ModernAppData) {
        val plain = toJson(data).toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plain)
        val encoded = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)
        val temp = context.filesDir.resolve("$fileName.tmp")
        temp.writeText(encoded)
        val target = context.filesDir.resolve(fileName)
        if (target.exists()) target.delete()
        check(temp.renameTo(target))
    }

    fun exportPlainJson(): String = toJson(read()).toString()

    fun decodePlainJson(json: String): ModernAppData =
        normalizeModernData(fromJson(JSONObject(json)))

    fun importPlainJson(json: String): ModernAppData {
        val decoded = decodePlainJson(json)
        write(decoded)
        return decoded
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun toJson(data: ModernAppData): JSONObject {
        return JSONObject().apply {
            put("dataVersion", MODERN_DATA_VERSION)
            put("activeCardId", data.activeCardId)
            put("lastModifiedMillis", data.lastModifiedMillis)
            put("cards", JSONArray().apply {
                data.cards.forEach { card ->
                    put(JSONObject().apply {
                        put("id", card.id)
                        put("name", card.name)
                        put("closingDay", card.closingDay)
                        put("dueDay", card.dueDay)
                    })
                }
            })
            put("categories", JSONArray().apply { data.categories.forEach { put(it) } })
            put("recurringRules", JSONArray().apply {
                data.recurringRules.forEach { rule ->
                    put(JSONObject().apply {
                        put("id", rule.id)
                        put("cardId", rule.cardId)
                        put("amountCents", rule.amountCents)
                        put("startEpochDay", rule.startEpochDay)
                        put("dayOfMonth", rule.dayOfMonth)
                        put("category", rule.category)
                        put("note", rule.note)
                        put("active", rule.active)
                        put("createdAtMillis", rule.createdAtMillis)
                        put("skippedEpochDays", JSONArray().apply { rule.skippedEpochDays.forEach { put(it) } })
                    })
                }
            })
            put("purchases", JSONArray().apply {
                data.purchases.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("cardId", p.cardId)
                        put("amountCents", p.amountCents)
                        put("purchaseEpochDay", p.purchaseDate.toEpochDay())
                        put("createdAtMillis", p.createdAtMillis)
                        put("category", p.category)
                        put("note", p.note)
                        p.invoiceStartEpochDay?.let { put("invoiceStartEpochDay", it) }
                        p.invoiceEndEpochDay?.let { put("invoiceEndEpochDay", it) }
                        p.invoiceDueEpochDay?.let { put("invoiceDueEpochDay", it) }
                        p.installmentGroupId?.let { put("installmentGroupId", it) }
                        p.installmentNumber?.let { put("installmentNumber", it) }
                        p.installmentTotal?.let { put("installmentTotal", it) }
                        put("isRecurring", p.isRecurring)
                        p.recurringRuleId?.let { put("recurringRuleId", it) }
                    })
                }
            })
        }
    }

    private fun fromJson(root: JSONObject): ModernAppData {
        val dataVersion = root.optInt("dataVersion", 1)
        val cardsJson = root.optJSONArray("cards")
        val cards: List<ModernCardProfile>
        val activeCardId: String

        if (cardsJson == null || cardsJson.length() == 0) {
            val settingsJson = root.optJSONObject("settings") ?: JSONObject()
            val legacyStartDay = settingsJson.optInt("invoiceStartDay", 6).coerceIn(1, 31)
            val inferredClosing = if (legacyStartDay > 1) legacyStartDay - 1 else 31
            val legacyCard = ModernCardProfile(
                id = LEGACY_CARD_ID,
                name = "Cartão 1",
                closingDay = settingsJson.optInt("closingDay", inferredClosing).coerceIn(1, 31),
                dueDay = settingsJson.optInt("dueDay", 12).coerceIn(1, 31)
            )
            cards = listOf(legacyCard)
            activeCardId = legacyCard.id
        } else {
            cards = buildList {
                for (i in 0 until cardsJson.length()) {
                    val c = cardsJson.getJSONObject(i)
                    add(
                        ModernCardProfile(
                            id = c.optString("id", UUID.randomUUID().toString()),
                            name = c.optString("name", "Cartão ${i + 1}"),
                            closingDay = c.optInt("closingDay", 5).coerceIn(1, 31),
                            dueDay = c.optInt("dueDay", 12).coerceIn(1, 31)
                        )
                    )
                }
            }
            activeCardId = root.optString("activeCardId", cards.first().id).ifBlank { cards.first().id }
        }

        val categoriesJson = root.optJSONArray("categories")
        val categories = (if (categoriesJson == null) modernDefaultCategories else buildList {
            for (i in 0 until categoriesJson.length()) {
                val value = categoriesJson.optString(i).trim()
                if (value.isNotEmpty()) add(value)
            }
        }.ifEmpty { modernDefaultCategories }).toMutableList()

        if (dataVersion < 6) {
            if (categories.none { it.equals("Lanches", ignoreCase = true) }) categories.add("Lanches")
            if (categories.none { it.equals("Sorvetes", ignoreCase = true) }) categories.add("Sorvetes")
        }

        val firstCard = cards.first()
        val cardById = cards.associateBy { it.id }
        val recurringJson = root.optJSONArray("recurringRules") ?: JSONArray()
        val recurringRules = buildList {
            for (i in 0 until recurringJson.length()) {
                val r = recurringJson.getJSONObject(i)
                val skipped = r.optJSONArray("skippedEpochDays") ?: JSONArray()
                val skippedDays = buildList {
                    for (j in 0 until skipped.length()) add(skipped.optLong(j))
                }
                add(
                    ModernRecurringRule(
                        id = r.optString("id", UUID.randomUUID().toString()),
                        cardId = r.optString("cardId", firstCard.id).ifBlank { firstCard.id },
                        amountCents = r.optLong("amountCents", 0L),
                        startEpochDay = r.optLong("startEpochDay", LocalDate.now().toEpochDay()),
                        dayOfMonth = r.optInt("dayOfMonth", 1).coerceIn(1, 31),
                        category = r.optString("category", "Outros"),
                        note = r.optString("note", ""),
                        active = r.optBoolean("active", true),
                        skippedEpochDays = skippedDays,
                        createdAtMillis = r.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }
        }
        val purchasesJson = root.optJSONArray("purchases") ?: JSONArray()
        val purchases = buildList {
            for (i in 0 until purchasesJson.length()) {
                val p = purchasesJson.getJSONObject(i)
                val cardId = p.optString("cardId", firstCard.id).ifBlank { firstCard.id }
                val card = cardById[cardId] ?: firstCard
                var raw = ModernPurchase(
                    id = p.optString("id", UUID.randomUUID().toString()),
                    cardId = card.id,
                    amountCents = p.optLong("amountCents", 0L),
                    purchaseDate = LocalDate.ofEpochDay(p.optLong("purchaseEpochDay", LocalDate.now().toEpochDay())),
                    createdAtMillis = p.optLong("createdAtMillis", Instant.now().toEpochMilli()),
                    category = p.optString("category", "Outros"),
                    note = p.optString("note", ""),
                    invoiceStartEpochDay = if (p.has("invoiceStartEpochDay")) p.optLong("invoiceStartEpochDay") else null,
                    invoiceEndEpochDay = if (p.has("invoiceEndEpochDay")) p.optLong("invoiceEndEpochDay") else null,
                    invoiceDueEpochDay = if (p.has("invoiceDueEpochDay")) p.optLong("invoiceDueEpochDay") else null,
                    installmentGroupId = p.optString("installmentGroupId", "").ifBlank { null },
                    installmentNumber = if (p.has("installmentNumber")) p.optInt("installmentNumber") else null,
                    installmentTotal = if (p.has("installmentTotal")) p.optInt("installmentTotal") else null,
                    isRecurring = p.optBoolean("isRecurring", false),
                    recurringRuleId = p.optString("recurringRuleId", "").ifBlank { null }
                )

                if (dataVersion < 3 && raw.isInstallment) {
                    val number = raw.installmentNumber ?: 1
                    raw = modernAssignToPeriod(
                        raw.copy(invoiceStartEpochDay = null, invoiceEndEpochDay = null, invoiceDueEpochDay = null),
                        modernInstallmentPeriod(raw.purchaseDate, number, card)
                    )
                } else if (raw.invoiceStartEpochDay == null || raw.invoiceEndEpochDay == null || raw.invoiceDueEpochDay == null) {
                    raw = modernAssignInvoice(raw, card)
                } else if (dataVersion < 2) {
                    val closeDate = modernDateAtDay(raw.purchaseDate.year, raw.purchaseDate.monthValue, card.closingDay)
                    if (raw.purchaseDate == closeDate) {
                        raw = modernAssignInvoice(
                            raw.copy(invoiceStartEpochDay = null, invoiceEndEpochDay = null, invoiceDueEpochDay = null),
                            card
                        )
                    }
                }
                add(raw)
            }
        }

        return ModernAppData(
            cards = cards,
            activeCardId = activeCardId,
            purchases = purchases,
            recurringRules = recurringRules,
            categories = categories,
            lastModifiedMillis = root.optLong("lastModifiedMillis", 0L)
        )
    }
}
