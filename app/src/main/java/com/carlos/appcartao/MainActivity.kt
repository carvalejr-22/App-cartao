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
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
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
import kotlin.math.max

private const val DATA_VERSION = 3

private val AppColors = lightColorScheme(
    primary = Color(0xFF135D54),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5F5EC),
    onPrimaryContainer = Color(0xFF063B35),
    secondary = Color(0xFF4169E1),
    secondaryContainer = Color(0xFFE2E9FF),
    tertiary = Color(0xFFFF8A34),
    tertiaryContainer = Color(0xFFFFE6D4),
    background = Color(0xFFF5F7FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEF1F5),
    outlineVariant = Color(0xFFDDE2E8)
)

private val defaultCategories = listOf(
    "Mercado", "Padaria", "Posto de gasolina", "Estacionamento", "Transporte",
    "Restaurante", "Lazer", "Farmácia", "Saúde", "Casa", "Assinaturas",
    "Roupas", "Educação", "Viagem", "Outros"
)

private val chartColors = listOf(
    Color(0xFF4169E1), Color(0xFF12B76A), Color(0xFFFF8A34), Color(0xFF7A5AF8),
    Color(0xFF06AED4), Color(0xFFE04F5F), Color(0xFFF1C21B), Color(0xFF667085)
)

data class Purchase(
    val id: String = UUID.randomUUID().toString(),
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
    val installmentTotal: Int? = null
) {
    val isInstallment: Boolean
        get() = installmentGroupId != null && installmentNumber != null && installmentTotal != null && installmentTotal > 1
}

data class CardSettings(
    val closingDay: Int = 5,
    val dueDay: Int = 12
) {
    val bestPurchaseDay: Int get() = if (closingDay >= 31) 1 else closingDay + 1
}

data class AppData(
    val purchases: List<Purchase> = emptyList(),
    val settings: CardSettings = CardSettings(),
    val categories: List<String> = defaultCategories
)

data class InvoicePeriod(
    val start: LocalDate,
    val end: LocalDate,
    val dueDate: LocalDate
)

enum class Screen { HOME, INVOICE, ANALYSIS, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = SecureStore(this)
        setContent {
            MaterialTheme(colorScheme = AppColors) {
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
        val normalized = normalizeData(newData)
        store.write(normalized)
        data = normalized
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = screen == Screen.HOME,
                    onClick = { screen = Screen.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Início") }
                )
                NavigationBarItem(
                    selected = screen == Screen.INVOICE,
                    onClick = { screen = Screen.INVOICE },
                    icon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                    label = { Text("Fatura") }
                )
                NavigationBarItem(
                    selected = screen == Screen.ANALYSIS,
                    onClick = { screen = Screen.ANALYSIS },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                    label = { Text("Análises") }
                )
                NavigationBarItem(
                    selected = screen == Screen.SETTINGS,
                    onClick = { screen = Screen.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
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
                Screen.HOME -> HomeScreen(
                    data = data,
                    onAdd = { newPurchases ->
                        persist(data.copy(purchases = data.purchases + newPurchases))
                    }
                )

                Screen.INVOICE -> InvoiceScreen(
                    data = data,
                    onDelete = { id ->
                        persist(data.copy(purchases = data.purchases.filterNot { it.id == id }))
                    },
                    onUpdate = { purchase ->
                        val reassigned = assignInvoice(
                            purchase.copy(
                                invoiceStartEpochDay = null,
                                invoiceEndEpochDay = null,
                                invoiceDueEpochDay = null
                            ),
                            data.settings
                        )
                        persist(data.copy(purchases = data.purchases.map { if (it.id == reassigned.id) reassigned else it }))
                    }
                )

                Screen.ANALYSIS -> AnalysisScreen(data)
                Screen.SETTINGS -> SettingsScreen(
                    data = data,
                    onSaveSettings = { settings -> persist(data.copy(settings = settings)) },
                    onCategoriesChanged = { newCategories -> persist(data.copy(categories = newCategories)) }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(data: AppData, onAdd: (List<Purchase>) -> Unit) {
    val today = LocalDate.now()
    val period = invoiceForPurchase(today, data.settings)
    val total = purchasesForPeriod(data.purchases, period, data.settings).sumOf { it.amountCents }

    var amount by remember { mutableStateOf("") }
    var category by remember(data.categories) { mutableStateOf(data.categories.firstOrNull() ?: "Outros") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today) }
    var categoryOpen by remember { mutableStateOf(false) }
    var isInstallment by remember { mutableStateOf(false) }
    var installmentCount by remember { mutableStateOf("12") }
    var error by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        item { Text("Seu controle financeiro, simples e offline", color = Color(0xFF667085)) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Fatura atual", color = Color.White.copy(alpha = .82f))
                            Text(
                                formatMoney(total),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = .14f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Wallet, contentDescription = null, tint = Color.White)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        InvoiceInfo("Fecha", dayLabel(period.end), Icons.Default.CalendarMonth)
                        InvoiceInfo("Vence", dayLabel(period.dueDate), Icons.Default.CreditCard)
                        InvoiceInfo("Melhor compra", dayLabel(period.end.plusDays(1)), Icons.Default.TrendingUp)
                    }
                }
            }
        }
        item { Text("Nova compra", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
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
            Box(modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(
                    onClick = { categoryOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(categoryIcon(category), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(category, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Alterar")
                }
                DropdownMenu(expanded = categoryOpen, onDismissRequest = { categoryOpen = false }) {
                    data.categories.forEach { item ->
                        DropdownMenuItem(
                            leadingIcon = { Icon(categoryIcon(item), contentDescription = null) },
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
                shape = RoundedCornerShape(16.dp),
                label = { Text("Descrição (opcional)") },
                singleLine = true
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isInstallment) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Compra parcelada", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Repete o valor automaticamente nas próximas faturas",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF667085)
                            )
                        }
                        Switch(checked = isInstallment, onCheckedChange = { isInstallment = it; error = null })
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
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(formatDate(date))
                }
            }
        }
        if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error) }
        item {
            Button(
                onClick = {
                    val cents = parseMoneyToCents(amount)
                    val parcels = if (isInstallment) installmentCount.toIntOrNull() else 1
                    when {
                        cents == null || cents <= 0 -> error = "Digite um valor válido. Ex.: 228,38"
                        parcels == null || parcels !in 2..60 -> error = "Informe entre 2 e 60 parcelas."
                        else -> {
                            val base = Purchase(
                                amountCents = cents,
                                purchaseDate = date,
                                category = category,
                                note = note.trim()
                            )
                            val newPurchases = if (parcels == 1) {
                                listOf(assignInvoice(base, data.settings))
                            } else {
                                buildInstallmentSeries(base, parcels, data.settings)
                            }
                            onAdd(newPurchases)
                            amount = ""
                            note = ""
                            date = LocalDate.now()
                            isInstallment = false
                            installmentCount = "12"
                            error = null
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isInstallment) "Salvar parcelas" else "Salvar compra", fontWeight = FontWeight.Bold)
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun InvoiceInfo(title: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.Start) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = .78f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(3.dp))
        Text(title, color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InvoiceScreen(
    data: AppData,
    onDelete: (String) -> Unit,
    onUpdate: (Purchase) -> Unit
) {
    val periods = availableInvoicePeriods(data)
    val currentPeriod = invoiceForPurchase(LocalDate.now(), data.settings)
    var selectedEnd by remember { mutableStateOf(currentPeriod.end) }
    val selectedIndexRaw = periods.indexOfFirst { it.end == selectedEnd }
    val selectedIndex = if (selectedIndexRaw >= 0) selectedIndexRaw else periods.indexOfFirst { it.end == currentPeriod.end }.coerceAtLeast(0)
    val period = periods.getOrElse(selectedIndex) { currentPeriod }
    val purchases = purchasesForPeriod(data.purchases, period, data.settings)
        .sortedWith(compareByDescending<Purchase> { it.purchaseDate }.thenByDescending { it.createdAtMillis })
    val total = purchases.sumOf { it.amountCents }
    var invoiceMenuOpen by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Purchase?>(null) }
    var editingPurchase by remember { mutableStateOf<Purchase?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Spacer(Modifier.height(6.dp)) }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Faturas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(periodStatus(period, currentPeriod), color = Color(0xFF667085))
                }
                Box {
                    FilledTonalButton(onClick = { invoiceMenuOpen = true }) {
                        Text(invoiceMonthLabel(period))
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = invoiceMenuOpen, onDismissRequest = { invoiceMenuOpen = false }) {
                        periods.forEach { item ->
                            val itemTotal = purchasesForPeriod(data.purchases, item, data.settings).sumOf { it.amountCents }
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(invoiceMonthLabel(item), fontWeight = FontWeight.SemiBold)
                                        Text(formatMoney(itemTotal), style = MaterialTheme.typography.bodySmall, color = Color(0xFF667085))
                                    }
                                },
                                onClick = { selectedEnd = item.end; invoiceMenuOpen = false }
                            )
                        }
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (selectedIndex < periods.lastIndex) selectedEnd = periods[selectedIndex + 1].end },
                    enabled = selectedIndex < periods.lastIndex
                ) { Icon(Icons.Default.ChevronLeft, contentDescription = "Fatura anterior") }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(invoiceMonthLabel(period), fontWeight = FontWeight.Bold)
                    Text(
                        "Compras ${formatDate(period.start)} a ${formatDate(period.end.minusDays(1))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF667085)
                    )
                }
                IconButton(
                    onClick = { if (selectedIndex > 0) selectedEnd = periods[selectedIndex - 1].end },
                    enabled = selectedIndex > 0
                ) { Icon(Icons.Default.ChevronRight, contentDescription = "Fatura seguinte") }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Total da fatura", color = Color(0xFF475467))
                    Text(formatMoney(total), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Fecha ${formatDate(period.end)} • vence ${formatDate(period.dueDate)}")
                }
            }
        }
        if (purchases.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Text("Nenhuma compra lançada nesta fatura.", modifier = Modifier.padding(18.dp))
                }
            }
        } else {
            items(purchases, key = { it.id }) { purchase ->
                PurchaseRow(
                    purchase = purchase,
                    onEdit = { editingPurchase = purchase },
                    onDelete = { pendingDelete = purchase }
                )
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    pendingDelete?.let { purchase ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (purchase.isInstallment) "Excluir esta parcela?" else "Excluir compra?") },
            text = {
                Text(
                    if (purchase.isInstallment) {
                        "Será excluída apenas a parcela ${purchase.installmentNumber}/${purchase.installmentTotal} de ${purchase.note.ifBlank { purchase.category }}."
                    } else {
                        "${purchase.note.ifBlank { purchase.category }} — ${formatMoney(purchase.amountCents)}"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(purchase.id); pendingDelete = null }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } }
        )
    }

    editingPurchase?.let { purchase ->
        EditPurchaseDialog(
            purchase = purchase,
            categories = data.categories,
            onDismiss = { editingPurchase = null },
            onSave = {
                onUpdate(it)
                selectedEnd = purchasePeriod(it, data.settings).end
                editingPurchase = null
            }
        )
    }
}

@Composable
private fun PurchaseRow(purchase: Purchase, onEdit: () -> Unit, onDelete: () -> Unit) {
    val installment = purchase.isInstallment
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (installment) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .55f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(42.dp).background(
                    if (installment) MaterialTheme.colorScheme.secondary.copy(alpha = .12f) else MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (installment) Icons.Default.Repeat else categoryIcon(purchase.category),
                    contentDescription = null,
                    tint = if (installment) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    purchase.note.ifBlank { purchase.category },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (installment) {
                    Text(
                        "Parcela ${purchase.installmentNumber}/${purchase.installmentTotal} • ${purchase.category}",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Compra em ${formatDate(purchase.purchaseDate)}", color = Color(0xFF667085), style = MaterialTheme.typography.labelSmall)
                } else {
                    Text("${purchase.category} • ${formatDate(purchase.purchaseDate)}", color = Color(0xFF667085), style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(formatMoney(purchase.amountCents), fontWeight = FontWeight.Bold)
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color(0xFFB42318))
            }
        }
    }
}

@Composable
private fun EditPurchaseDialog(
    purchase: Purchase,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (Purchase) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var amount by remember(purchase.id) { mutableStateOf(formatMoneyForInput(purchase.amountCents)) }
    var category by remember(purchase.id) { mutableStateOf(purchase.category) }
    var note by remember(purchase.id) { mutableStateOf(purchase.note) }
    var date by remember(purchase.id) { mutableStateOf(purchase.purchaseDate) }
    var categoryOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val available = (categories + purchase.category).distinct()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (purchase.isInstallment) "Editar parcela" else "Editar compra") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (purchase.isInstallment) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Parcela ${purchase.installmentNumber}/${purchase.installmentTotal}. A edição altera somente esta parcela.",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it; error = null },
                    label = { Text("Valor") },
                    prefix = { Text("R$ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { categoryOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(categoryIcon(category), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(category, modifier = Modifier.weight(1f), maxLines = 1)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = categoryOpen, onDismissRequest = { categoryOpen = false }) {
                        available.forEach { item ->
                            DropdownMenuItem(
                                leadingIcon = { Icon(categoryIcon(item), contentDescription = null) },
                                text = { Text(item) },
                                onClick = { category = item; categoryOpen = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Descrição") },
                    singleLine = true
                )
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(formatDate(date))
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = parseMoneyToCents(amount)
                if (cents == null || cents <= 0) {
                    error = "Informe um valor válido."
                } else {
                    onSave(
                        purchase.copy(
                            amountCents = cents,
                            purchaseDate = date,
                            category = category,
                            note = note.trim(),
                            invoiceStartEpochDay = null,
                            invoiceEndEpochDay = null,
                            invoiceDueEpochDay = null
                        )
                    )
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun AnalysisScreen(data: AppData) {
    val today = LocalDate.now()
    val current = invoiceForPurchase(today, data.settings)
    val currentPurchases = purchasesForPeriod(data.purchases, current, data.settings)
    val total = currentPurchases.sumOf { it.amountCents }
    val categoryTotals = currentPurchases.groupBy { it.category }
        .mapValues { (_, values) -> values.sumOf { it.amountCents } }
        .toList()
        .sortedByDescending { it.second }

    val periods = analysisPeriods(data)
    val periodTotals = periods.map { p -> purchasesForPeriod(data.purchases, p, data.settings).sumOf { it.amountCents } }
    val futurePeriods = availableInvoicePeriods(data).filter { it.end.isAfter(current.end) }
    val futureCommitted = futurePeriods.sumOf { p -> purchasesForPeriod(data.purchases, p, data.settings).sumOf { it.amountCents } }
    var showTrend by remember { mutableStateOf(true) }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Spacer(Modifier.height(6.dp)) }
        item {
            Text("Análises", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Histórico preservado para comparar seu comportamento de compra", color = Color(0xFF667085))
        }
        if (futureCommitted > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Compromissos futuros", fontWeight = FontWeight.Bold)
                            Text(
                                "${futurePeriods.size} faturas futuras • ${formatMoney(futureCommitted)} já previstos",
                                color = Color(0xFF475467)
                            )
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Últimas faturas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Tendência", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(6.dp))
                        Switch(checked = showTrend, onCheckedChange = { showTrend = it })
                    }
                    Spacer(Modifier.height(12.dp))
                    MultiColorBarChart(periodTotals, showTrend)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        periods.forEach { Text(shortPeriodLabel(it), style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Gastos por categoria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    if (categoryTotals.isEmpty()) {
                        Text("Ainda não há dados para analisar.")
                    } else {
                        CategoryPieChart(categoryTotals)
                        Spacer(Modifier.height(14.dp))
                        categoryTotals.forEachIndexed { index, (category, cents) ->
                            val pct = if (total > 0) cents * 100.0 / total else 0.0
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(chartColors[index % chartColors.size], CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Icon(categoryIcon(category), contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(category, modifier = Modifier.weight(1f))
                                Text(
                                    "${formatMoney(cents)} • ${String.format(Locale("pt", "BR"), "%.0f", pct)}%",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (index < categoryTotals.lastIndex) {
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Resumo da fatura atual", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("${currentPurchases.size} lançamentos • ${formatMoney(total)}")
                    if (currentPurchases.isNotEmpty()) {
                        Text("Ticket médio: ${formatMoney(total / currentPurchases.size)}")
                        Text("Maior categoria: ${categoryTotals.firstOrNull()?.first ?: "—"}")
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun MultiColorBarChart(values: List<Long>, showTrend: Boolean) {
    val guideColor = MaterialTheme.colorScheme.outlineVariant
    val trendColor = MaterialTheme.colorScheme.primary
    val maxValue = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        drawLine(guideColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
        if (values.isEmpty()) return@Canvas
        val slot = size.width / values.size
        val barWidth = slot * .56f
        values.forEachIndexed { index, value ->
            val height = size.height * .86f * (value.toFloat() / maxValue.toFloat())
            val x = index * slot + (slot - barWidth) / 2f
            drawRect(
                color = chartColors[index % chartColors.size],
                topLeft = Offset(x, size.height - height),
                size = Size(barWidth, height)
            )
        }
        if (showTrend && values.size >= 2) {
            val n = values.size.toDouble()
            val sumX = values.indices.sumOf { it.toDouble() }
            val sumY = values.sumOf { it.toDouble() }
            val sumXY = values.indices.sumOf { it.toDouble() * values[it].toDouble() }
            val sumXX = values.indices.sumOf { it.toDouble() * it.toDouble() }
            val denominator = n * sumXX - sumX * sumX
            val slope = if (denominator == 0.0) 0.0 else (n * sumXY - sumX * sumY) / denominator
            val intercept = (sumY - slope * sumX) / n
            fun yFor(index: Int): Float {
                val predicted = max(0.0, intercept + slope * index)
                val h = size.height * .86f * (predicted.toFloat() / maxValue.toFloat())
                return size.height - h
            }
            val startX = slot / 2f
            val endX = size.width - slot / 2f
            drawLine(
                color = trendColor,
                start = Offset(startX, yFor(0)),
                end = Offset(endX, yFor(values.lastIndex)),
                strokeWidth = 5f
            )
            drawCircle(trendColor, radius = 6f, center = Offset(startX, yFor(0)))
            drawCircle(trendColor, radius = 6f, center = Offset(endX, yFor(values.lastIndex)))
        }
    }
}

@Composable
private fun CategoryPieChart(categoryTotals: List<Pair<String, Long>>) {
    val total = categoryTotals.sumOf { it.second }.coerceAtLeast(1L)
    Canvas(modifier = Modifier.fillMaxWidth().height(190.dp)) {
        val diameter = size.minDimension * .88f
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        var startAngle = -90f
        categoryTotals.forEachIndexed { index, (_, cents) ->
            val sweep = 360f * (cents.toFloat() / total.toFloat())
            drawArc(
                color = chartColors[index % chartColors.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = diameter * .24f)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun SettingsScreen(
    data: AppData,
    onSaveSettings: (CardSettings) -> Unit,
    onCategoriesChanged: (List<String>) -> Unit
) {
    val settings = data.settings
    var closingDay by remember(settings) { mutableStateOf(settings.closingDay.toString()) }
    var dueDay by remember(settings) { mutableStateOf(settings.dueDay.toString()) }
    var message by remember { mutableStateOf<String?>(null) }
    var newCategory by remember { mutableStateOf("") }
    val current = invoiceForPurchase(LocalDate.now(), settings)

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Spacer(Modifier.height(6.dp)) }
        item {
            Text("Ajustes do cartão", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Compras no dia do fechamento já entram na próxima fatura.", color = Color(0xFF667085))
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = closingDay,
                        onValueChange = { closingDay = it.filter(Char::isDigit).take(2); message = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        label = { Text("Dia do fechamento") },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dueDay,
                        onValueChange = { dueDay = it.filter(Char::isDigit).take(2); message = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        label = { Text("Dia do vencimento") },
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                            Text("Ciclo da fatura atual", fontWeight = FontWeight.Bold)
                            Text("Compras ${formatDate(current.start)} a ${formatDate(current.end.minusDays(1))}")
                            Text("Fecha ${formatDate(current.end)} • vence ${formatDate(current.dueDate)}")
                            Text("Melhor compra: ${dayLabel(current.end.plusDays(1))}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Button(
                        onClick = {
                            val c = closingDay.toIntOrNull()
                            val d = dueDay.toIntOrNull()
                            if (c == null || d == null || c !in 1..31 || d !in 1..31) {
                                message = "Informe dias entre 1 e 31."
                            } else {
                                onSaveSettings(CardSettings(c, d))
                                message = "Configurações salvas. O histórico antigo foi preservado."
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Salvar configurações", fontWeight = FontWeight.Bold) }
                    message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Categorias", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Ícones diferentes facilitam a identificação visual.", color = Color(0xFF667085), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newCategory,
                            onValueChange = { newCategory = it.take(28) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Nova categoria") },
                            singleLine = true
                        )
                        IconButton(onClick = {
                            val cleaned = newCategory.trim()
                            if (cleaned.isNotEmpty() && data.categories.none { it.equals(cleaned, ignoreCase = true) }) {
                                onCategoriesChanged((data.categories + cleaned).distinct())
                                newCategory = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Adicionar categoria", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    data.categories.forEachIndexed { index, category ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(categoryIcon(category), contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(category, modifier = Modifier.weight(1f))
                            if (category != "Outros") {
                                IconButton(onClick = {
                                    val updated = data.categories.filterNot { it == category }
                                    onCategoriesChanged(if (updated.isEmpty()) listOf("Outros") else updated)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir categoria", tint = Color(0xFFB42318))
                                }
                            }
                        }
                        if (index < data.categories.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
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
                    Text("Privacidade e segurança", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("• Funciona sem internet.")
                    Text("• Dados ficam no armazenamento privado do Android.")
                    Text("• Arquivo protegido com AES-GCM e chave no Android Keystore.")
                    Text("• Não salve número do cartão, CVV ou senha no campo de descrição.")
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

private fun normalizeData(data: AppData): AppData {
    val safeCategories = data.categories.map { it.trim() }.filter { it.isNotEmpty() }.distinct().toMutableList()
    if (safeCategories.none { it == "Outros" }) safeCategories.add("Outros")
    val normalizedPurchases = data.purchases.map { assignInvoice(it, data.settings) }
    return data.copy(purchases = normalizedPurchases, categories = safeCategories)
}

private fun assignInvoice(purchase: Purchase, settings: CardSettings): Purchase {
    if (purchase.invoiceStartEpochDay != null && purchase.invoiceEndEpochDay != null && purchase.invoiceDueEpochDay != null) {
        return purchase
    }
    return assignToPeriod(purchase, invoiceForPurchase(purchase.purchaseDate, settings))
}

private fun assignToPeriod(purchase: Purchase, period: InvoicePeriod): Purchase = purchase.copy(
    invoiceStartEpochDay = period.start.toEpochDay(),
    invoiceEndEpochDay = period.end.toEpochDay(),
    invoiceDueEpochDay = period.dueDate.toEpochDay()
)

private fun buildInstallmentSeries(base: Purchase, count: Int, settings: CardSettings): List<Purchase> {
    val groupId = UUID.randomUUID().toString()
    val result = mutableListOf<Purchase>()
    var period = invoiceForPurchase(base.purchaseDate, settings)
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
        result.add(assignToPeriod(installment, period))
        period = nextInvoicePeriod(period, settings)
    }
    return result
}

private fun nextInvoicePeriod(period: InvoicePeriod, settings: CardSettings): InvoicePeriod {
    val nextMonth = period.end.plusMonths(1)
    val end = dateAtDay(nextMonth.year, nextMonth.monthValue, settings.closingDay)
    val previousMonth = end.minusMonths(1)
    val start = dateAtDay(previousMonth.year, previousMonth.monthValue, settings.closingDay)
    val dueBase = if (settings.dueDay > settings.closingDay) end else end.plusMonths(1)
    val dueDate = dateAtDay(dueBase.year, dueBase.monthValue, settings.dueDay)
    return InvoicePeriod(start = start, end = end, dueDate = dueDate)
}

private fun installmentPeriod(
    purchaseDate: LocalDate,
    installmentNumber: Int,
    settings: CardSettings
): InvoicePeriod {
    var period = invoiceForPurchase(purchaseDate, settings)
    repeat((installmentNumber - 1).coerceAtLeast(0)) {
        period = nextInvoicePeriod(period, settings)
    }
    return period
}

private fun purchasePeriod(purchase: Purchase, settings: CardSettings): InvoicePeriod {
    val start = purchase.invoiceStartEpochDay
    val end = purchase.invoiceEndEpochDay
    val due = purchase.invoiceDueEpochDay
    return if (start != null && end != null && due != null) {
        InvoicePeriod(LocalDate.ofEpochDay(start), LocalDate.ofEpochDay(end), LocalDate.ofEpochDay(due))
    } else {
        invoiceForPurchase(purchase.purchaseDate, settings)
    }
}

private fun availableInvoicePeriods(data: AppData): List<InvoicePeriod> {
    val current = invoiceForPurchase(LocalDate.now(), data.settings)
    val fromPurchases = data.purchases.map { purchasePeriod(it, data.settings) }
    val previous = invoiceForPurchase(current.start.minusDays(1), data.settings)
    return (listOf(current, previous) + fromPurchases)
        .distinctBy { it.end }
        .sortedByDescending { it.end }
}

private fun analysisPeriods(data: AppData): List<InvoicePeriod> {
    val current = invoiceForPurchase(LocalDate.now(), data.settings)
    val available = availableInvoicePeriods(data)
        .filter { !it.end.isAfter(current.end) }
        .toMutableList()
    var cursor = current
    if (available.none { it.end == cursor.end }) available.add(cursor)
    while (available.size < 6) {
        cursor = invoiceForPurchase(cursor.start.minusDays(1), data.settings)
        if (available.none { it.end == cursor.end }) available.add(cursor)
    }
    return available.sortedBy { it.end }.takeLast(6)
}

private fun invoiceForPurchase(date: LocalDate, settings: CardSettings): InvoicePeriod {
    val closeThisMonth = dateAtDay(date.year, date.monthValue, settings.closingDay)

    // A compra realizada no próprio dia do fechamento já pertence à próxima fatura.
    val end = if (!date.isBefore(closeThisMonth)) {
        val next = date.plusMonths(1)
        dateAtDay(next.year, next.monthValue, settings.closingDay)
    } else {
        closeThisMonth
    }

    val previousMonth = end.minusMonths(1)
    val previousClose = dateAtDay(previousMonth.year, previousMonth.monthValue, settings.closingDay)
    val start = previousClose

    val dueBase = if (settings.dueDay > settings.closingDay) end else end.plusMonths(1)
    val dueDate = dateAtDay(dueBase.year, dueBase.monthValue, settings.dueDay)
    return InvoicePeriod(start = start, end = end, dueDate = dueDate)
}

private fun purchasesForPeriod(
    purchases: List<Purchase>,
    period: InvoicePeriod,
    settings: CardSettings
): List<Purchase> = purchases.filter { purchasePeriod(it, settings).end == period.end }

private fun periodStatus(period: InvoicePeriod, current: InvoicePeriod): String = when {
    period.end.isAfter(current.end) -> "Fatura futura • valores já previstos"
    period.end.isBefore(current.end) -> "Fatura fechada e armazenada"
    else -> "Fatura atual"
}

private fun dateAtDay(year: Int, month: Int, requestedDay: Int): LocalDate {
    val ym = YearMonth.of(year, month)
    return LocalDate.of(year, month, requestedDay.coerceIn(1, ym.lengthOfMonth()))
}

private fun categoryIcon(category: String): ImageVector = when (category) {
    "Mercado" -> Icons.Default.ShoppingCart
    "Padaria" -> Icons.Default.BakeryDining
    "Posto de gasolina" -> Icons.Default.LocalGasStation
    "Estacionamento" -> Icons.Default.LocalParking
    "Transporte" -> Icons.Default.DirectionsBus
    "Restaurante" -> Icons.Default.Restaurant
    "Lazer" -> Icons.Default.SportsEsports
    "Farmácia" -> Icons.Default.Medication
    "Saúde" -> Icons.Default.Favorite
    "Casa" -> Icons.Default.Home
    "Assinaturas" -> Icons.Default.Subscriptions
    "Roupas" -> Icons.Default.Checkroom
    "Educação" -> Icons.Default.School
    "Viagem" -> Icons.Default.Flight
    else -> Icons.Default.Category
}

private fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
private fun dayLabel(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd/MM"))
private fun invoiceMonthLabel(period: InvoicePeriod): String =
    period.dueDate.format(DateTimeFormatter.ofPattern("MMM/yyyy", Locale("pt", "BR"))).replaceFirstChar { it.uppercase() }
private fun shortPeriodLabel(period: InvoicePeriod): String =
    period.dueDate.format(DateTimeFormatter.ofPattern("MMM", Locale("pt", "BR"))).replaceFirstChar { it.uppercase() }

private fun formatMoney(cents: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(BigDecimal(cents).divide(BigDecimal(100)))
}

private fun formatMoneyForInput(cents: Long): String =
    BigDecimal(cents).divide(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',')

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
            normalizeData(fromJson(JSONObject(json)))
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
        root.put("dataVersion", DATA_VERSION)
        root.put("settings", JSONObject().apply {
            put("closingDay", data.settings.closingDay)
            put("dueDay", data.settings.dueDay)
        })
        root.put("categories", JSONArray().apply { data.categories.forEach { put(it) } })
        root.put("purchases", JSONArray().apply {
            data.purchases.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
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
                })
            }
        })
        return root
    }

    private fun fromJson(root: JSONObject): AppData {
        val dataVersion = root.optInt("dataVersion", 1)
        val settingsJson = root.optJSONObject("settings") ?: JSONObject()
        val legacyStartDay = settingsJson.optInt("invoiceStartDay", 6).coerceIn(1, 31)
        val inferredClosing = if (legacyStartDay > 1) legacyStartDay - 1 else 31
        val settings = CardSettings(
            closingDay = settingsJson.optInt("closingDay", inferredClosing).coerceIn(1, 31),
            dueDay = settingsJson.optInt("dueDay", 12).coerceIn(1, 31)
        )
        val categoriesJson = root.optJSONArray("categories")
        val storedCategories = if (categoriesJson == null) {
            defaultCategories
        } else {
            buildList {
                for (i in 0 until categoriesJson.length()) {
                    val value = categoriesJson.optString(i).trim()
                    if (value.isNotEmpty()) add(value)
                }
            }.ifEmpty { defaultCategories }
        }

        val array = root.optJSONArray("purchases") ?: JSONArray()
        val purchases = buildList {
            for (i in 0 until array.length()) {
                val p = array.getJSONObject(i)
                var raw = Purchase(
                    id = p.optString("id", UUID.randomUUID().toString()),
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
                    installmentTotal = if (p.has("installmentTotal")) p.optInt("installmentTotal") else null
                )

                if (dataVersion < 2) {
            val closingDateInPurchaseMonth = dateAtDay(raw.purchaseDate.year, raw.purchaseDate.monthValue, settings.closingDay)
            if (raw.purchaseDate == closingDateInPurchaseMonth) {
                raw = raw.copy(
                    invoiceStartEpochDay = null,
                    invoiceEndEpochDay = null,
                    invoiceDueEpochDay = null
                )
            } else if (raw.invoiceEndEpochDay != null) {
                val oldEnd = LocalDate.ofEpochDay(raw.invoiceEndEpochDay)
                val previousMonth = oldEnd.minusMonths(1)
                val correctedStart = dateAtDay(previousMonth.year, previousMonth.monthValue, settings.closingDay)
                raw = raw.copy(invoiceStartEpochDay = correctedStart.toEpochDay())
            }
        }

        // v3 corrige parcelamentos históricos deslocados para a fatura errada.
        // A parcela 1 sempre nasce na primeira fatura definida pela data real da compra.
        if (dataVersion < 3 && raw.isInstallment) {
            val number = raw.installmentNumber ?: 1
            val correctedPeriod = installmentPeriod(raw.purchaseDate, number, settings)
            raw = assignToPeriod(
                raw.copy(
                    invoiceStartEpochDay = null,
                    invoiceEndEpochDay = null,
                    invoiceDueEpochDay = null
                ),
                correctedPeriod
            )
        }

                add(assignInvoice(raw, settings))
            }
        }
        return AppData(purchases, settings, storedCategories)
    }
}
