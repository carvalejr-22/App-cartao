from pathlib import Path

path = Path('app/src/main/java/com/carlos/appcartao/ModernCardActivity.kt')
text = path.read_text(encoding='utf-8')

old = '''            val callback = pendingReceiptCallback
            val uri = pendingReceiptUri
            if (callback == null || uri == null) {
                clearReceiptCapture()
                return@registerForActivityResult
            }
'''
new = '''            val callback = pendingReceiptCallback
            val uri = pendingReceiptUri
            val file = pendingReceiptFile
            if (callback == null || uri == null || file == null) {
                clearReceiptCapture()
                return@registerForActivityResult
            }
'''
assert old in text, 'camera result prelude not found'
text = text.replace(old, new, 1)

old = '''            ReceiptOcrScanner(this).scan(uri, pendingReceiptCategories) { result, error ->
                runOnUiThread {
                    callback(result, error)
                    clearReceiptCapture()
                }
            }
'''
new = '''            ReceiptOcrScanner().scan(file, pendingReceiptCategories) { result, error ->
                runOnUiThread {
                    callback(result, error)
                    clearReceiptCapture()
                }
            }
'''
assert old in text, 'old receipt scanner call not found'
text = text.replace(old, new, 1)

old = '''    val current = modernInvoiceForPurchase(LocalDate.now(), card)
    val isMainCard = card.id == data.cards.first().id

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
'''
new = '''    val current = modernInvoiceForPurchase(LocalDate.now(), card)
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
'''
assert old in text, 'settings status insertion point not found'
text = text.replace(old, new, 1)

old = '''                            Text("✓ Proteção automática", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                "Se estiver offline, o pedido de backup aguarda a conexão. O Android faz o envio em segundo plano no momento permitido pelo sistema.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF667085)
                            )
'''
new = '''                            Text("✓ Proteção automática", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
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
'''
assert old in text, 'backup info card not found'
text = text.replace(old, new, 1)

marker = '''private fun formatModernDate(date: LocalDate): String = date.format(modernFullDateFormatter)
'''
insert = '''private fun formatAutomaticBackupMoment(millis: Long): String =
    java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(java.util.Date(millis))

private fun formatModernDate(date: LocalDate): String = date.format(modernFullDateFormatter)
'''
assert marker in text, 'format helper insertion point not found'
text = text.replace(marker, insert, 1)

path.write_text(text, encoding='utf-8')
