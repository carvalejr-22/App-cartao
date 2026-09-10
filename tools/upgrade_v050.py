from pathlib import Path

activity = Path('app/src/main/java/com/carlos/appcartao/ModernCardActivity.kt')
text = activity.read_text()

# Keep the add-card action discreet in the upper-right corner.
old_icon = '''                        Box(
                            modifier = Modifier.size(42.dp).background(Color.White.copy(alpha = .14f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text("▣", color = Color.White, fontSize = 22.sp) }'''
new_icon = '''                        TextButton(
                            onClick = onAddCard,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Text("+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
                        }'''
if old_icon not in text:
    raise SystemExit('summary action block not found')
text = text.replace(old_icon, new_icon, 1)

# With a single card, do not show an unnecessary card strip. The + button above is enough.
old_cards = '''        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Cartões", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onAddCard, modifier = Modifier.size(44.dp)) {
                    Text("+", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Light)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                data.cards.forEach { card ->
                    val period = modernInvoiceForPurchase(today, card)
                    val total = modernPurchasesForPeriod(data.purchases, card.id, period).sumOf { it.amountCents }
                    val selected = card.id == activeCard.id
                    FilledTonalButton(
                        onClick = { onSelectCard(card.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(card.name, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                            Text(formatModernMoney(total), style = MaterialTheme.typography.labelSmall, color = Color(0xFF667085))
                        }
                    }
                }
            }
        }
'''
new_cards = '''        if (data.cards.size > 1) {
            item {
                Text("Cartões", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    data.cards.forEach { card ->
                        val period = modernInvoiceForPurchase(today, card)
                        val total = modernPurchasesForPeriod(data.purchases, card.id, period).sumOf { it.amountCents }
                        val selected = card.id == activeCard.id
                        FilledTonalButton(
                            onClick = { onSelectCard(card.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(card.name, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                Text(formatModernMoney(total), style = MaterialTheme.typography.labelSmall, color = Color(0xFF667085))
                            }
                        }
                    }
                }
            }
        }
'''
if old_cards not in text:
    raise SystemExit('card selector block not found')
text = text.replace(old_cards, new_cards, 1)

analysis_start = text.find('@Composable\nprivate fun ModernAnalysisScreen')
settings_start = text.find('@Composable\nprivate fun ModernSettingsScreen', analysis_start)
if analysis_start < 0 or settings_start < 0:
    raise SystemExit('analysis region not found')

new_analysis = r'''@Composable
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

'''
text = text[:analysis_start] + new_analysis + text[settings_start:]

# Cache formatters to avoid recreating them on every recomposition.
formatter_anchor = '''private val modernChartColors = listOf(
    Color(0xFF4B67D1), Color(0xFF16A36A), Color(0xFFFF8B3D), Color(0xFF8657D8),
    Color(0xFF1598B5), Color(0xFFD85868), Color(0xFFD2A51C), Color(0xFF667085)
)
'''
formatter_insert = formatter_anchor + '''
private val modernFullDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val modernDayMonthFormatter = DateTimeFormatter.ofPattern("dd/MM")
private val modernInvoiceMonthFormatter = DateTimeFormatter.ofPattern("MMM/yyyy", Locale("pt", "BR"))
private val modernShortMonthFormatter = DateTimeFormatter.ofPattern("MMM", Locale("pt", "BR"))
private val modernCurrencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
'''
if formatter_anchor not in text:
    raise SystemExit('formatter anchor not found')
text = text.replace(formatter_anchor, formatter_insert, 1)

old_formatters = '''private fun formatModernDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
private fun modernDayLabel(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd/MM"))
private fun modernInvoiceMonthLabel(period: ModernInvoicePeriod): String =
    period.dueDate.format(DateTimeFormatter.ofPattern("MMM/yyyy", Locale("pt", "BR"))).replaceFirstChar { it.uppercase() }
private fun modernShortPeriodLabel(period: ModernInvoicePeriod): String =
    period.dueDate.format(DateTimeFormatter.ofPattern("MMM", Locale("pt", "BR"))).replaceFirstChar { it.uppercase() }

private fun formatModernMoney(cents: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(BigDecimal(cents).divide(BigDecimal(100)))
}
'''
new_formatters = '''private fun formatModernDate(date: LocalDate): String = date.format(modernFullDateFormatter)
private fun modernDayLabel(date: LocalDate): String = date.format(modernDayMonthFormatter)
private fun modernInvoiceMonthLabel(period: ModernInvoicePeriod): String =
    period.dueDate.format(modernInvoiceMonthFormatter).replaceFirstChar { it.uppercase() }
private fun modernShortPeriodLabel(period: ModernInvoicePeriod): String =
    period.dueDate.format(modernShortMonthFormatter).replaceFirstChar { it.uppercase() }

private fun formatModernMoney(cents: Long): String =
    modernCurrencyFormatter.format(BigDecimal(cents).divide(BigDecimal(100)))
'''
if old_formatters not in text:
    raise SystemExit('old formatter functions not found')
text = text.replace(old_formatters, new_formatters, 1)

activity.write_text(text)

# Reduce APK size. The new activity uses text/vector drawing instead of the extended icon pack.
gradle = Path('app/build.gradle.kts')
g = gradle.read_text()
if 'versionCode = 5' not in g or 'versionName = "0.4.1"' not in g:
    raise SystemExit('unexpected app version')
g = g.replace('versionCode = 5', 'versionCode = 6', 1)
g = g.replace('versionName = "0.4.1"', 'versionName = "0.5.0"', 1)
g = g.replace('''    buildTypes {
        release {''', '''    buildTypes {
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {''', 1)
g = g.replace('    implementation("androidx.compose.ui:ui-tooling-preview")\n', '')
g = g.replace('    implementation("androidx.compose.material:material-icons-extended")\n', '')
g = g.replace('    debugImplementation("androidx.compose.ui:ui-tooling")\n', '')
gradle.write_text(g)

# Remove the old activity entirely: it is no longer launched and would keep dead UI code + icon dependencies in the APK.
legacy = Path('app/src/main/java/com/carlos/appcartao/MainActivity.kt')
if legacy.exists():
    legacy.unlink()
