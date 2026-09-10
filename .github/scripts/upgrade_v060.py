from pathlib import Path

path = Path('app/src/main/java/com/carlos/appcartao/ModernCardActivity.kt')
text = path.read_text()


def replace_once(old: str, new: str, label: str):
    global text
    if old not in text:
        raise SystemExit(f'{label} not found')
    text = text.replace(old, new, 1)


replace_once(
    'import androidx.activity.ComponentActivity\nimport androidx.activity.compose.setContent\n',
    'import androidx.activity.ComponentActivity\nimport androidx.activity.compose.setContent\nimport androidx.activity.result.IntentSenderRequest\nimport androidx.activity.result.contract.ActivityResultContracts\n',
    'activity result imports'
)
replace_once(
    'import androidx.compose.material3.HorizontalDivider\n',
    'import androidx.compose.material3.HorizontalDivider\nimport androidx.compose.material3.IconButton\n',
    'IconButton import'
)
replace_once(
    'import androidx.compose.runtime.Composable\n',
    'import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.LaunchedEffect\n',
    'LaunchedEffect import'
)
replace_once('private const val MODERN_DATA_VERSION = 5\n', 'private const val MODERN_DATA_VERSION = 6\n', 'data version')
replace_once(
    '    "Mercado", "Padaria", "Posto de gasolina", "Estacionamento", "Transporte",\n    "Restaurante", "Lazer", "Farmácia", "Saúde", "Casa", "Assinaturas",\n',
    '    "Mercado", "Padaria", "Lanches", "Sorvetes", "Posto de gasolina", "Estacionamento", "Transporte",\n    "Restaurante", "Lazer", "Farmácia", "Saúde", "Casa", "Assinaturas",\n',
    'default categories'
)
replace_once(
    '    val purchases: List<ModernPurchase> = emptyList(),\n    val categories: List<String> = modernDefaultCategories\n)\n',
    '    val purchases: List<ModernPurchase> = emptyList(),\n    val categories: List<String> = modernDefaultCategories,\n    val lastModifiedMillis: Long = 0L\n)\n',
    'app data timestamp'
)

old = '''class ModernCardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = ModernSecureStore(this)
        setContent {
            MaterialTheme(colorScheme = ModernColors) {
                ModernCreditCardApp(store)
            }
        }
    }
}

@Composable
private fun ModernCreditCardApp(store: ModernSecureStore) {
    var data by remember { mutableStateOf(store.read()) }
    var screen by remember { mutableStateOf(ModernScreen.HOME) }

    fun persist(newData: ModernAppData) {
        val normalized = normalizeModernData(newData)
        store.write(normalized)
        data = normalized
    }
'''
new = '''class ModernCardActivity : ComponentActivity() {
    private lateinit var store: ModernSecureStore
    private lateinit var cloudSync: GoogleDriveSync
    private var pendingCloudCallback: ((CloudSyncResult) -> Unit)? = null

    private val cloudAuthorizationLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val callback = pendingCloudCallback
            if (result.resultCode != RESULT_OK || result.data == null) {
                pendingCloudCallback = null
                callback?.invoke(CloudSyncResult(false, "Conexão com a Conta Google cancelada."))
                return@registerForActivityResult
            }

            try {
                val authorization = cloudSync.authorizationResultFromIntent(result.data!!)
                finishCloudAuthorization(authorization, callback)
            } catch (_: Exception) {
                pendingCloudCallback = null
                callback?.invoke(CloudSyncResult(false, "Não foi possível concluir a autorização do Google Drive."))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ModernSecureStore(this)
        cloudSync = GoogleDriveSync(this)
        setContent {
            MaterialTheme(colorScheme = ModernColors) {
                ModernCreditCardApp(
                    store = store,
                    cloudSync = cloudSync,
                    onAuthorizeAndSync = ::authorizeAndSync,
                    onDisconnectCloud = ::disconnectCloud
                )
            }
        }
    }

    private fun authorizeAndSync(callback: (CloudSyncResult) -> Unit) {
        pendingCloudCallback = callback
        cloudSync.requestAuthorization { authorization, error ->
            runOnUiThread {
                when {
                    error != null || authorization == null -> {
                        pendingCloudCallback = null
                        callback(CloudSyncResult(false, "Não foi possível acessar a Conta Google."))
                    }
                    authorization.hasResolution() -> {
                        val pendingIntent = authorization.pendingIntent
                        if (pendingIntent == null) {
                            pendingCloudCallback = null
                            callback(CloudSyncResult(false, "O Google não retornou uma tela de autorização válida."))
                        } else {
                            cloudAuthorizationLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                            )
                        }
                    }
                    else -> finishCloudAuthorization(authorization, callback)
                }
            }
        }
    }

    private fun finishCloudAuthorization(
        authorization: com.google.android.gms.auth.api.identity.AuthorizationResult,
        callback: ((CloudSyncResult) -> Unit)?
    ) {
        val token = authorization.accessToken
        if (token.isNullOrBlank()) {
            pendingCloudCallback = null
            callback?.invoke(CloudSyncResult(false, "A Conta Google não forneceu autorização para o backup."))
            return
        }

        cloudSync.setEnabled(true)
        cloudSync.syncWithToken(store, token) { syncResult ->
            pendingCloudCallback = null
            callback?.invoke(syncResult)
        }
    }

    private fun disconnectCloud(callback: (String) -> Unit) {
        cloudSync.disconnect { message -> runOnUiThread { callback(message) } }
    }
}

@Composable
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

    fun persist(newData: ModernAppData) {
        val normalized = normalizeModernData(newData).copy(lastModifiedMillis = System.currentTimeMillis())
        store.write(normalized)
        data = normalized
        if (cloudSync.isEnabled()) cloudSync.enqueueSync()
    }
'''
replace_once(old, new, 'activity/cloud app block')

replace_once(
    '''                ModernScreen.INVOICE -> ModernInvoiceScreen(
                    data = data,
                    onSelectCard = ::selectCard,
                    onDelete = { id -> persist(data.copy(purchases = data.purchases.filterNot { it.id == id })) }
                )
''',
    '''                ModernScreen.INVOICE -> ModernInvoiceScreen(
                    data = data,
                    onSelectCard = ::selectCard,
                    onDelete = { id -> persist(data.copy(purchases = data.purchases.filterNot { it.id == id })) },
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
''',
    'invoice callbacks'
)

replace_once(
    '''                    },
                    onCategoriesChanged = { categories -> persist(data.copy(categories = categories)) }
                )
''',
    '''                    },
                    onCategoriesChanged = { categories -> persist(data.copy(categories = categories)) },
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
                )
''',
    'settings cloud callbacks'
)

replace_once(
    '''private fun ModernInvoiceScreen(
    data: ModernAppData,
    onSelectCard: (String) -> Unit,
    onDelete: (String) -> Unit
) {
''',
    '''private fun ModernInvoiceScreen(
    data: ModernAppData,
    onSelectCard: (String) -> Unit,
    onDelete: (String) -> Unit,
    onEditAmount: (String, Long) -> Unit
) {
''',
    'invoice signature'
)
replace_once(
    '    var pendingDelete by remember { mutableStateOf<ModernPurchase?>(null) }\n',
    '    var pendingDelete by remember { mutableStateOf<ModernPurchase?>(null) }\n    var pendingEdit by remember { mutableStateOf<ModernPurchase?>(null) }\n',
    'invoice edit state'
)
replace_once(
    '''                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatModernMoney(purchase.amountCents), fontWeight = FontWeight.Bold)
                            TextButton(onClick = { pendingDelete = purchase }) { Text("Excluir", color = Color(0xFFB42318)) }
                        }
''',
    '''                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatModernMoney(purchase.amountCents), fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { pendingEdit = purchase }) {
                                    Text("✎", fontSize = 21.sp, color = MaterialTheme.colorScheme.secondary)
                                }
                                IconButton(onClick = { pendingDelete = purchase }) {
                                    Text("🗑", fontSize = 19.sp, color = Color(0xFFB42318))
                                }
                            }
                        }
''',
    'invoice row actions'
)

marker = '''    pendingDelete?.let { purchase ->
        AlertDialog(
'''
edit_dialog = '''    pendingEdit?.let { purchase ->
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

'''
if marker not in text:
    raise SystemExit('delete dialog marker not found')
text = text.replace(marker, edit_dialog + marker, 1)

replace_once(
    '''private fun ModernSettingsScreen(
    data: ModernAppData,
    onSelectCard: (String) -> Unit,
    onSaveCard: (ModernCardProfile) -> Unit,
    onDeleteCard: (String) -> Unit,
    onCategoriesChanged: (List<String>) -> Unit
) {
''',
    '''private fun ModernSettingsScreen(
    data: ModernAppData,
    onSelectCard: (String) -> Unit,
    onSaveCard: (ModernCardProfile) -> Unit,
    onDeleteCard: (String) -> Unit,
    onCategoriesChanged: (List<String>) -> Unit,
    cloudEnabled: Boolean,
    cloudMessage: String?,
    onCloudSync: () -> Unit,
    onCloudDisconnect: () -> Unit
) {
''',
    'settings signature cloud'
)
replace_once(
    '''                            if (category != "Outros") {
                                TextButton(onClick = {
                                    val updated = data.categories.filterNot { it == category }
                                    onCategoriesChanged(if (updated.isEmpty()) listOf("Outros") else updated)
                                }) { Text("Excluir", color = Color(0xFFB42318)) }
                            }
''',
    '''                            if (category != "Outros") {
                                IconButton(onClick = {
                                    val updated = data.categories.filterNot { it == category }
                                    onCategoriesChanged(if (updated.isEmpty()) listOf("Outros") else updated)
                                }) {
                                    Text("🗑", fontSize = 18.sp, color = Color(0xFFB42318))
                                }
                            }
''',
    'category trash icon'
)

privacy_marker = '''        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Leve e privado", fontWeight = FontWeight.Bold)
'''
cloud_card = '''        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Backup na Conta Google", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (cloudEnabled) {
                            "Ativado. O app continua funcionando offline e sincroniza as mudanças quando houver internet."
                        } else {
                            "Opcional. Conecte uma Conta Google para restaurar seus dados ao trocar de celular ou reinstalar o app."
                        },
                        color = Color(0xFF667085),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = onCloudSync,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(if (cloudEnabled) "Sincronizar agora" else "Conectar Conta Google", fontWeight = FontWeight.Bold)
                    }
                    if (cloudEnabled) {
                        OutlinedButton(
                            onClick = onCloudDisconnect,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Desconectar backup") }
                    }
                    cloudMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
'''
if privacy_marker not in text:
    raise SystemExit('privacy marker not found')
text = text.replace(privacy_marker, cloud_card + privacy_marker, 1)
replace_once(
    '                    Text("• Sem internet e sem serviços rodando em segundo plano.")\n',
    '                    Text("• Funciona offline; a internet só é usada quando o backup Google está autorizado.")\n                    Text("• A sincronização usa a pasta privada appData do Google Drive, invisível no Meu Drive.")\n',
    'privacy text'
)
replace_once(
    '    "Padaria" -> "🥖"\n',
    '    "Padaria" -> "🥖"\n    "Lanches" -> "🍔"\n    "Sorvetes" -> "🍦"\n',
    'category symbols'
)
replace_once(
    'private class ModernSecureStore(private val context: Context) {\n',
    'class ModernSecureStore(private val context: Context) {\n',
    'store visibility'
)
replace_once(
    '''            val json = String(cipher.doFinal(encrypted), Charsets.UTF_8)
            normalizeModernData(fromJson(JSONObject(json)))
        } catch (_: Exception) {
''',
    '''            val json = String(cipher.doFinal(encrypted), Charsets.UTF_8)
            val decoded = normalizeModernData(fromJson(JSONObject(json)))
            if (decoded.lastModifiedMillis > 0L) decoded
            else decoded.copy(lastModifiedMillis = file.lastModified().coerceAtLeast(1L))
        } catch (_: Exception) {
''',
    'legacy timestamp read'
)
replace_once(
    '''        check(temp.renameTo(target))
    }

    private fun getOrCreateKey(): SecretKey {
''',
    '''        check(temp.renameTo(target))
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
''',
    'store export methods'
)
replace_once(
    '            put("activeCardId", data.activeCardId)\n',
    '            put("activeCardId", data.activeCardId)\n            put("lastModifiedMillis", data.lastModifiedMillis)\n',
    'serialize timestamp'
)
replace_once(
    '''        val categoriesJson = root.optJSONArray("categories")
        val categories = if (categoriesJson == null) modernDefaultCategories else buildList {
            for (i in 0 until categoriesJson.length()) {
                val value = categoriesJson.optString(i).trim()
                if (value.isNotEmpty()) add(value)
            }
        }.ifEmpty { modernDefaultCategories }
''',
    '''        val categoriesJson = root.optJSONArray("categories")
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
''',
    'category migration'
)
replace_once(
    '        return ModernAppData(cards = cards, activeCardId = activeCardId, purchases = purchases, categories = categories)\n',
    '''        return ModernAppData(
            cards = cards,
            activeCardId = activeCardId,
            purchases = purchases,
            categories = categories,
            lastModifiedMillis = root.optLong("lastModifiedMillis", 0L)
        )
''',
    'deserialize timestamp'
)

path.write_text(text)
print('ModernCardActivity upgraded to v0.6.0')
