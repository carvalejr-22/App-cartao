package com.carlos.appcartao

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

data class CloudSyncResult(
    val success: Boolean,
    val message: String,
    val restoredFromCloud: Boolean = false
)

class GoogleDriveSync(private val context: Context) {
    companion object {
        private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val BACKUP_FILE = "meu_cartao_backup_v1.json"
        private const val PREFS = "google_drive_sync"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_LAST_STATUS = "last_status"
        private const val UNIQUE_SYNC_WORK = "meu_cartao_google_drive_sync"
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun scope() = Scope(DRIVE_SCOPE)

    private fun authorizationRequest(): AuthorizationRequest =
        AuthorizationRequest.builder()
            .setRequestedScopes(listOf(scope()))
            .build()

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun lastStatusText(): String? = prefs.getString(KEY_LAST_STATUS, null)

    private fun saveStatus(message: String) {
        prefs.edit().putString(KEY_LAST_STATUS, message).apply()
    }

    fun requestAuthorization(callback: (AuthorizationResult?, Exception?) -> Unit) {
        Identity.getAuthorizationClient(context)
            .authorize(authorizationRequest())
            .addOnSuccessListener { callback(it, null) }
            .addOnFailureListener { callback(null, it) }
    }

    fun authorizationResultFromIntent(intent: Intent): AuthorizationResult =
        Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(intent)

    fun syncWithToken(
        store: ModernSecureStore,
        accessToken: String,
        callback: (CloudSyncResult) -> Unit
    ) {
        executor.execute {
            val result = try {
                syncBlocking(store, accessToken)
            } catch (e: Exception) {
                CloudSyncResult(false, friendlyError(e))
            }
            saveStatus(result.message)
            mainHandler.post { callback(result) }
        }
    }

    fun tryRestoreExistingGrant(
        store: ModernSecureStore,
        callback: (CloudSyncResult?) -> Unit
    ) {
        requestAuthorization { authorization, error ->
            if (error != null || authorization == null || authorization.hasResolution()) {
                mainHandler.post { callback(null) }
                return@requestAuthorization
            }
            val token = authorization.accessToken
            if (token.isNullOrBlank()) {
                mainHandler.post { callback(null) }
                return@requestAuthorization
            }

            setEnabled(true)
            syncWithToken(store, token, callback = callback)
        }
    }

    fun enqueueSync() {
        if (!isEnabled()) return
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<GoogleDriveSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_SYNC_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun disconnect(callback: (String) -> Unit) {
        setEnabled(false)
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_SYNC_WORK)

        val request = RevokeAccessRequest.builder()
            .setScopes(listOf(scope()))
            .build()

        Identity.getAuthorizationClient(context)
            .revokeAccess(request)
            .addOnCompleteListener {
                val message = if (it.isSuccessful) {
                    "Backup desconectado da Conta Google."
                } else {
                    "Sincronização automática desativada neste aparelho."
                }
                saveStatus(message)
                mainHandler.post { callback(message) }
            }
    }

    internal fun syncBlocking(store: ModernSecureStore, accessToken: String): CloudSyncResult {
        val local = store.read()
        val remoteFileId = findBackupFile(accessToken)

        if (remoteFileId == null) {
            return if (local.lastModifiedMillis > 0L) {
                createBackup(accessToken, store.exportPlainJson())
                CloudSyncResult(true, "Backup criado na Conta Google.")
            } else {
                CloudSyncResult(true, "Conta Google conectada. O backup será criado na primeira alteração.")
            }
        }

        val remoteJson = downloadBackup(accessToken, remoteFileId)
        val remote = store.decodePlainJson(remoteJson)

        return when {
            remote.lastModifiedMillis > local.lastModifiedMillis -> {
                store.write(remote)
                CloudSyncResult(true, "Dados restaurados da Conta Google.", restoredFromCloud = true)
            }
            local.lastModifiedMillis > remote.lastModifiedMillis -> {
                updateBackup(accessToken, remoteFileId, store.exportPlainJson())
                CloudSyncResult(true, "Backup atualizado na Conta Google.")
            }
            else -> CloudSyncResult(true, "Backup já está atualizado.")
        }
    }

    internal fun authorizationBlocking(): AuthorizationResult =
        Tasks.await(Identity.getAuthorizationClient(context).authorize(authorizationRequest()))

    private fun findBackupFile(accessToken: String): String? {
        val q = URLEncoder.encode("name='$BACKUP_FILE' and trashed=false", StandardCharsets.UTF_8.name())
        val fields = URLEncoder.encode("files(id,name,modifiedTime)", StandardCharsets.UTF_8.name())
        val url = "https://www.googleapis.com/drive/v3/files" +
            "?spaces=appDataFolder&pageSize=1&q=$q&fields=$fields"
        val response = request("GET", url, accessToken)
        val files = JSONObject(response).optJSONArray("files") ?: JSONArray()
        if (files.length() == 0) return null
        return files.getJSONObject(0).optString("id").ifBlank { null }
    }

    private fun downloadBackup(accessToken: String, fileId: String): String =
        request(
            "GET",
            "https://www.googleapis.com/drive/v3/files/$fileId?alt=media",
            accessToken
        )

    private fun createBackup(accessToken: String, json: String) {
        val boundary = "----MeuCartao${System.currentTimeMillis()}"
        val metadata = JSONObject()
            .put("name", BACKUP_FILE)
            .put("parents", JSONArray().put("appDataFolder"))
            .put("mimeType", "application/json")
            .toString()

        val body = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata)
            append("\r\n--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(json)
            append("\r\n--$boundary--\r\n")
        }.toByteArray(StandardCharsets.UTF_8)

        request(
            "POST",
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id",
            accessToken,
            body,
            "multipart/related; boundary=$boundary"
        )
    }

    private fun updateBackup(accessToken: String, fileId: String, json: String) {
        request(
            "PATCH",
            "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media",
            accessToken,
            json.toByteArray(StandardCharsets.UTF_8),
            "application/json; charset=UTF-8"
        )
    }

    private fun request(
        method: String,
        url: String,
        accessToken: String,
        body: ByteArray? = null,
        contentType: String? = null
    ): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")
            connection.useCaches = false

            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", contentType ?: "application/json")
                connection.outputStream.use { it.write(body) }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IOException("Google Drive HTTP $code: ${response.take(240)}")
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun friendlyError(error: Exception): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("401") -> "A autorização do Google expirou. Abra Ajustes e conecte a Conta Google novamente."
            message.contains("403") -> "O Google Drive ainda não está habilitado para este aplicativo."
            message.contains("timeout", ignoreCase = true) -> "A internet está lenta. A sincronização será tentada novamente."
            else -> "Não foi possível sincronizar agora. Os dados continuam seguros no aparelho."
        }
    }
}

class GoogleDriveSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val sync = GoogleDriveSync(applicationContext)
        if (!sync.isEnabled()) return Result.success()

        return try {
            val authorization = sync.authorizationBlocking()
            if (authorization.hasResolution() || authorization.accessToken.isNullOrBlank()) {
                Result.success()
            } else {
                val result = sync.syncBlocking(
                    ModernSecureStore(applicationContext),
                    authorization.accessToken!!
                )
                if (result.success) Result.success() else Result.retry()
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
