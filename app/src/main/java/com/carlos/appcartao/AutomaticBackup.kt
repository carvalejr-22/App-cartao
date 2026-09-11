package com.carlos.appcartao

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

internal data class AutomaticBackupStatus(
    val pending: Boolean,
    val lastChangeMillis: Long,
    val lastRequestMillis: Long,
    val lastCompletedMillis: Long,
    val lastRestoreMillis: Long
)

internal object AutomaticBackupStatusStore {
    private const val PREFS = "automatic_backup_status"
    private const val KEY_PENDING = "pending"
    private const val KEY_LAST_CHANGE = "last_change"
    private const val KEY_LAST_REQUEST = "last_request"
    private const val KEY_LAST_COMPLETED = "last_completed"
    private const val KEY_LAST_RESTORE = "last_restore"

    fun markChanged(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_PENDING, true)
            .putLong(KEY_LAST_CHANGE, System.currentTimeMillis())
            .apply()
    }

    fun markRequested(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAST_REQUEST, System.currentTimeMillis())
            .apply()
    }

    fun markCompleted(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_PENDING, false)
            .putLong(KEY_LAST_COMPLETED, System.currentTimeMillis())
            .apply()
    }

    fun markRestored(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_PENDING, false)
            .putLong(KEY_LAST_RESTORE, System.currentTimeMillis())
            .apply()
    }

    fun read(context: Context): AutomaticBackupStatus {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return AutomaticBackupStatus(
            pending = prefs.getBoolean(KEY_PENDING, false),
            lastChangeMillis = prefs.getLong(KEY_LAST_CHANGE, 0L),
            lastRequestMillis = prefs.getLong(KEY_LAST_REQUEST, 0L),
            lastCompletedMillis = prefs.getLong(KEY_LAST_COMPLETED, 0L),
            lastRestoreMillis = prefs.getLong(KEY_LAST_RESTORE, 0L)
        )
    }
}

object AutomaticBackupScheduler {
    private const val UNIQUE_WORK = "meu_cartao_android_backup_request"

    fun schedule(context: Context) {
        AutomaticBackupStatusStore.markChanged(context.applicationContext)
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
        AutomaticBackupStatusStore.markRequested(applicationContext)
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
        AutomaticBackupStatusStore.markCompleted(this)
    }

    override fun onRestore(
        data: BackupDataInput,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?
    ) {
        var restored = false
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
                restored = runCatching {
                    ModernSecureStore(this).importPlainJson(String(payload, Charsets.UTF_8))
                    true
                }.getOrDefault(false)
            }
        }
        if (restored) AutomaticBackupStatusStore.markRestored(this)
    }
}
