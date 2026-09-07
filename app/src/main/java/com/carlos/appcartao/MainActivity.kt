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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

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
    val closingDay: Int = 5,
    val dueDay: Int = 12
) {
    val bestPurchaseDay: Int get() = if (closingDay >= 31) 1 else closingDay + 1
}

data class AppData(
    val purchases: List<Purchase> = emptyList(),
    val settings: CardSettings = CardSettings()
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
        store.write(newData)
        data = newData
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
    val period = invoiceForPurchase(today, data.settings)
    val total = invoicePurchases(data.purchases, period).sumOf { it.amountCents }

    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(categories.first()) }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today) }
    var categoryOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val selectedInvoice = invoiceForPurchase(date, data.settings)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Column {
                Text("Meu Cartão", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Seu controle financeiro, simples e offline", color = Color(0xFF667085))
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
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
                            modifier = Modifier.size(46.dp).background(Color.White.copy(alpha = .14f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Wallet, contentDescription = null, tint = Color.White)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        InvoiceInfo("Fecha", dayLabel(period.end), Icons.Default.CalendarMonth)
                        InvoiceInfo("Vence", dayLabel(period.dueDate), Icons.Default.CreditCard)
                        InvoiceInfo("Melhor compra", dayLabel(period.end.plusDays(1)), Icons.Default.TrendingUp)
                    }
                }
            }
        }
        item {
            Text("Nova compra", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                label = { Text("Valor da compra") },
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
                    Text(category, modifier = Modifier.weight(1f))
                    Text("Alterar")
                }
                DropdownMenu(expanded = categoryOpen, onDismissRequest = { categoryOpen = false }) {
                    categories.forEach { item ->
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
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Esta compra entra na fatura", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Fechamento ${formatDate(selectedInvoice.end)} • vencimento ${formatDate(selectedInvoice.dueDate)}",
                        color = Color(0xFF344054)
                    )
                }
            }
        }
        if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error) }
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
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Salvar compra", fontWeight = FontWeight.Bold)
            }
        }
        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun InvoiceInfo(title: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.Start) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = .78f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(title, color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InvoiceScreen(data: AppData, onDelete: (String) -> Unit) {
    val period = invoiceForPurchase(LocalDate.now(), data.settings)
    val purchases = invoicePurchases(data.purchases, period)
        .sortedWith(compareByDescending<Purchase> { it.purchaseDate }.thenByDescending { it.createdAtMillis })
    val total = purchases.sumOf { it.amountCents }
    var pendingDelete by remember { mutableStateOf<Purchase?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text("Fatura atual", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Vencimento ${formatDate(period.dueDate)}", color = Color(0xFF667085))
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
                    Spacer(Modifier.height(6.dp))
                    Text("Compras de ${formatDate(period.start)} até ${formatDate(period.end)}")
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
                PurchaseRow(purchase = purchase, onDelete = { pendingDelete = purchase })
            }
        }
        item { Spacer(Modifier.height(18.dp)) }
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
private fun PurchaseRow(purchase: Purchase, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(categoryIcon(purchase.category), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(purchase.note.ifBlank { purchase.category }, fontWeight = FontWeight.SemiBold)
                Text("${purchase.category} • ${formatDate(purchase.purchaseDate)}", color = Color(0xFF667085), style = MaterialTheme.typography.bodySmall)
            }
            Text(formatMoney(purchase.amountCents), fontWeight = FontWeight.Bold)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color(0xFFB42318))
            }
        }
    }
}

@Composable
private fun AnalysisScreen(data: AppData) {
    val today = LocalDate.now()
    val current = invoiceForPurchase(today, data.settings)
    val currentPurchases = invoicePurchases(data.purchases, current)
    val total = currentPurchases.sumOf { it.amountCents }
    val categoryTotals = currentPurchases.groupBy { it.category }
        .mapValues { (_, values) -> values.sumOf { it.amountCents } }
        .toList()
        .sortedByDescending { it.second }

    val periods = (5 downTo 0).map { offset ->
        val ref = today.minusMonths(offset.toLong())
        invoiceForPurchase(ref, data.settings)
    }.distinctBy { it.end }
    val periodTotals = periods.map { p -> invoicePurchases(data.purchases, p).sumOf { it.amountCents } }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text("Análises", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Entenda para onde seu dinheiro está indo", color = Color(0xFF667085))
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Últimas faturas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(14.dp))
                    SimpleBarChart(periodTotals)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        periods.forEach { Text(periodLabel(it), style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Por categoria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    if (categoryTotals.isEmpty()) {
                        Text("Ainda não há dados para analisar.")
                    } else {
                        categoryTotals.forEach { (category, cents) ->
                            val pct = if (total > 0) cents * 100.0 / total else 0.0
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(categoryIcon(category), contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(category, modifier = Modifier.weight(1f))
                                Text("${formatMoney(cents)} • ${String.format(Locale("pt", "BR"), "%.0f", pct)}%", fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(9.dp))
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Resumo da fatura", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("${currentPurchases.size} compras • ${formatMoney(total)}")
                    if (currentPurchases.isNotEmpty()) {
                        Text("Ticket médio: ${formatMoney(total / currentPurchases.size)}")
                        Text("Maior categoria: ${categoryTotals.firstOrNull()?.first ?: "—"}")
                    }
                }
            }
        }
        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun SimpleBarChart(values: List<Long>) {
    val barColor = MaterialTheme.colorScheme.secondary
    val guideColor = MaterialTheme.colorScheme.outlineVariant
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        drawLine(guideColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
        if (values.isEmpty()) return@Canvas
        val slot = size.width / values.size
        val barWidth = slot * .58f
        values.forEachIndexed { index, value ->
            val height = size.height * (value.toFloat() / max.toFloat())
            val x = index * slot + (slot - barWidth) / 2f
            drawRect(barColor, topLeft = Offset(x, size.height - height), size = Size(barWidth, height))
        }
    }
}

@Composable
private fun SettingsScreen(settings: CardSettings, onSave: (CardSettings) -> Unit) {
    var closingDay by remember(settings) { mutableStateOf(settings.closingDay.toString()) }
    var dueDay by remember(settings) { mutableStateOf(settings.dueDay.toString()) }
    var message by remember { mutableStateOf<String?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text("Ajustes do cartão", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Informe apenas fechamento e vencimento. O restante é automático.", color = Color(0xFF667085))
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
                    Text("Melhor dia para compra: ${bestDayText(closingDay.toIntOrNull())}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Compras feitas depois do fechamento são lançadas automaticamente na próxima fatura.", color = Color(0xFF667085))
                    Button(
                        onClick = {
                            val c = closingDay.toIntOrNull()
                            val d = dueDay.toIntOrNull()
                            if (c == null || d == null || c !in 1..31 || d !in 1..31) {
                                message = "Informe dias entre 1 e 31."
                            } else {
                                onSave(CardSettings(c, d))
                                message = "Configurações salvas."
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
        item { Spacer(Modifier.height(18.dp)) }
    }
}

private fun invoiceForPurchase(date: LocalDate, settings: CardSettings): InvoicePeriod {
    val closeThisMonth = dateAtDay(date.year, date.monthValue, settings.closingDay)
    val end = if (date.isAfter(closeThisMonth)) {
        val next = date.plusMonths(1)
        dateAtDay(next.year, next.monthValue, settings.closingDay)
    } else closeThisMonth

    val previousMonth = end.minusMonths(1)
    val previousClose = dateAtDay(previousMonth.year, previousMonth.monthValue, settings.closingDay)
    val start = previousClose.plusDays(1)

    val dueBase = if (settings.dueDay > settings.closingDay) end else end.plusMonths(1)
    val dueDate = dateAtDay(dueBase.year, dueBase.monthValue, settings.dueDay)
    return InvoicePeriod(start = start, end = end, dueDate = dueDate)
}

private fun invoicePurchases(purchases: List<Purchase>, period: InvoicePeriod): List<Purchase> =
    purchases.filter { !it.purchaseDate.isBefore(period.start) && !it.purchaseDate.isAfter(period.end) }

private fun dateAtDay(year: Int, month: Int, requestedDay: Int): LocalDate {
    val ym = YearMonth.of(year, month)
    return LocalDate.of(year, month, requestedDay.coerceIn(1, ym.lengthOfMonth()))
}

private fun categoryIcon(category: String): ImageVector = when (category) {
    "Mercado" -> Icons.Default.ShoppingCart
    "Padaria", "Restaurante" -> Icons.Default.Restaurant
    "Posto de gasolina" -> Icons.Default.LocalGasStation
    "Estacionamento" -> Icons.Default.LocalParking
    "Transporte", "Viagem" -> Icons.Default.Train
    "Farmácia", "Saúde" -> Icons.Default.LocalHospital
    "Casa" -> Icons.Default.Home
    else -> Icons.Default.Storefront
}

private fun bestDayText(closingDay: Int?): String = when {
    closingDay == null -> "—"
    closingDay >= 31 -> "dia 1"
    else -> "dia ${closingDay + 1}"
}

private fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
private fun dayLabel(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd/MM"))
private fun periodLabel(period: InvoicePeriod): String =
    period.dueDate.format(DateTimeFormatter.ofPattern("MMM/yy", Locale("pt", "BR"))).replaceFirstChar { it.uppercase() }

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
            put("closingDay", data.settings.closingDay)
            put("dueDay", data.settings.dueDay)
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
        val legacyBestDay = settingsJson.optInt("bestPurchaseDay", 6).coerceIn(1, 31)
        val legacyStartDay = settingsJson.optInt("invoiceStartDay", 6).coerceIn(1, 31)
        val inferredClosing = if (legacyStartDay > 1) legacyStartDay - 1 else 31
        val settings = CardSettings(
            closingDay = settingsJson.optInt("closingDay", inferredClosing).coerceIn(1, 31),
            dueDay = settingsJson.optInt("dueDay", 12).coerceIn(1, 31)
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
