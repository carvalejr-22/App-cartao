from pathlib import Path

activity = Path('app/src/main/java/com/carlos/appcartao/ModernCardActivity.kt')
text = activity.read_text(encoding='utf-8')

old_launcher = '''    private val cloudAuthorizationLauncher =
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
'''

new_launcher = '''    private val cloudAuthorizationLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val callback = pendingCloudCallback
            if (callback == null) return@registerForActivityResult

            // Some Google Play Services versions can return an authorization payload
            // even when the Activity result code is not RESULT_OK. Always inspect the
            // payload first so a valid grant is not incorrectly reported as cancelled.
            result.data?.let { intent ->
                try {
                    val authorization = cloudSync.authorizationResultFromIntent(intent)
                    finishCloudAuthorization(authorization, callback)
                    return@registerForActivityResult
                } catch (error: Exception) {
                    if (result.resultCode == RESULT_OK) {
                        pendingCloudCallback = null
                        callback(CloudSyncResult(false, cloudSync.authorizationErrorMessage(error)))
                        return@registerForActivityResult
                    }
                }
            }

            // Re-check silently after the authorization UI closes. If access was
            // granted, finish normally. If the user really cancelled, Google will
            // still report a resolution as required. Configuration errors are now
            // surfaced instead of being mislabeled as cancellation.
            recheckAuthorizationAfterResolution(
                callback = callback,
                userCancelled = result.resultCode != RESULT_OK
            )
        }
'''

if old_launcher not in text:
    raise SystemExit('cloudAuthorizationLauncher block not found')
text = text.replace(old_launcher, new_launcher, 1)

old_error_branch = '''                    error != null || authorization == null -> {
                        pendingCloudCallback = null
                        callback(CloudSyncResult(false, "Não foi possível acessar a Conta Google."))
                    }
'''
new_error_branch = '''                    error != null || authorization == null -> {
                        pendingCloudCallback = null
                        val message = if (error != null) {
                            cloudSync.authorizationErrorMessage(error)
                        } else {
                            "O Google não retornou uma autorização válida."
                        }
                        callback(CloudSyncResult(false, message))
                    }
'''
if old_error_branch not in text:
    raise SystemExit('authorize error branch not found')
text = text.replace(old_error_branch, new_error_branch, 1)

anchor = '''    private fun finishCloudAuthorization(
        authorization: com.google.android.gms.auth.api.identity.AuthorizationResult,
        callback: ((CloudSyncResult) -> Unit)?
    ) {
'''
helper = '''    private fun recheckAuthorizationAfterResolution(
        callback: ((CloudSyncResult) -> Unit)?,
        userCancelled: Boolean
    ) {
        cloudSync.requestAuthorization { authorization, error ->
            runOnUiThread {
                when {
                    error != null -> {
                        pendingCloudCallback = null
                        callback?.invoke(CloudSyncResult(false, cloudSync.authorizationErrorMessage(error)))
                    }
                    authorization != null && !authorization.hasResolution() && !authorization.accessToken.isNullOrBlank() -> {
                        finishCloudAuthorization(authorization, callback)
                    }
                    else -> {
                        pendingCloudCallback = null
                        val message = if (userCancelled) {
                            "Conexão com a Conta Google cancelada."
                        } else {
                            "A Conta Google ainda não autorizou o backup. Tente novamente."
                        }
                        callback?.invoke(CloudSyncResult(false, message))
                    }
                }
            }
        }
    }

'''
if anchor not in text:
    raise SystemExit('finishCloudAuthorization anchor not found')
text = text.replace(anchor, helper + anchor, 1)

# Make the invoice action controls even less intrusive while keeping a usable tap target.
text = text.replace(
'''                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clickable { pendingEdit = purchase },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Editar valor",
                                    tint = Color(0xFF667085),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clickable { pendingDelete = purchase },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Excluir compra",
                                    tint = Color(0xFFB42318),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
''',
'''                            Box(
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
''',
1)

text = text.replace(
'''                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clickable {
                                            val updated = data.categories.filterNot { it == category }
                                            onCategoriesChanged(if (updated.isEmpty()) listOf("Outros") else updated)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "Excluir categoria",
                                        tint = Color(0xFFB42318),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
''',
'''                                Box(
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
''',
1)

activity.write_text(text, encoding='utf-8')

sync = Path('app/src/main/java/com/carlos/appcartao/GoogleDriveSync.kt')
sync_text = sync.read_text(encoding='utf-8')
sync_text = sync_text.replace(
    'import com.google.android.gms.common.api.Scope\n',
    'import com.google.android.gms.common.api.ApiException\nimport com.google.android.gms.common.api.CommonStatusCodes\nimport com.google.android.gms.common.api.Scope\n',
    1,
)

friendly_anchor = '''    private fun friendlyError(error: Exception): String {
'''
auth_helper = '''    fun authorizationErrorMessage(error: Exception): String {
        val apiError = error as? ApiException
        return when (apiError?.statusCode) {
            CommonStatusCodes.CANCELED -> "Conexão com a Conta Google cancelada."
            CommonStatusCodes.DEVELOPER_ERROR ->
                "O backup Google ainda não está configurado para a assinatura desta versão do app. Cadastre o pacote com o SHA-1 correto no Google Cloud."
            CommonStatusCodes.NETWORK_ERROR ->
                "Não foi possível falar com o Google. Verifique a internet e tente novamente."
            CommonStatusCodes.SIGN_IN_REQUIRED ->
                "Selecione uma Conta Google e autorize o backup."
            CommonStatusCodes.RESOLUTION_REQUIRED ->
                "A Conta Google precisa de uma autorização adicional. Tente conectar novamente."
            else -> {
                val status = apiError?.let { CommonStatusCodes.getStatusCodeString(it.statusCode) }
                if (status.isNullOrBlank()) {
                    "Não foi possível concluir a autorização da Conta Google."
                } else {
                    "Não foi possível concluir a autorização da Conta Google ($status)."
                }
            }
        }
    }

'''
if friendly_anchor not in sync_text:
    raise SystemExit('friendlyError anchor not found')
sync_text = sync_text.replace(friendly_anchor, auth_helper + friendly_anchor, 1)
sync.write_text(sync_text, encoding='utf-8')

build = Path('app/build.gradle.kts')
build_text = build.read_text(encoding='utf-8')
build_text = build_text.replace('versionCode = 9', 'versionCode = 10', 1)
build_text = build_text.replace('versionName = "0.6.1"', 'versionName = "0.6.2"', 1)
build.write_text(build_text, encoding='utf-8')

print('v0.6.2 patch applied')
