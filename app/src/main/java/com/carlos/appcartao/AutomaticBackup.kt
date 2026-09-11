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
