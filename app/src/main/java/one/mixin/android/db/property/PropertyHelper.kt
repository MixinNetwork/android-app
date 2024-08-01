package one.mixin.android.db.property

import android.os.Build
import one.mixin.android.Constants.Account.Migration.PREF_MIGRATION_ATTACHMENT
import one.mixin.android.Constants.Account.Migration.PREF_MIGRATION_BACKUP
import one.mixin.android.Constants.Account.Migration.PREF_MIGRATION_COLLECTION
import one.mixin.android.Constants.Account.Migration.PREF_MIGRATION_INSCRIPTION
import one.mixin.android.Constants.Account.Migration.PREF_MIGRATION_TRANSCRIPT_ATTACHMENT
import one.mixin.android.Constants.Account.Migration.PREF_MIGRATION_TRANSCRIPT_ATTACHMENT_LAST
import one.mixin.android.Constants.Account.PREF_BACKUP
import one.mixin.android.Constants.Account.PREF_CLEANUP_QUOTE_CONTENT
import one.mixin.android.Constants.Account.PREF_CLEANUP_THUMB
import one.mixin.android.Constants.Account.PREF_DUPLICATE_TRANSFER
import one.mixin.android.Constants.Account.PREF_STRANGER_TRANSFER
import one.mixin.android.Constants.BackUp.BACKUP_LAST_TIME
import one.mixin.android.Constants.BackUp.BACKUP_PERIOD
import one.mixin.android.Constants.Download.AUTO_DOWNLOAD_MOBILE
import one.mixin.android.Constants.Download.AUTO_DOWNLOAD_ROAMING
import one.mixin.android.Constants.Download.AUTO_DOWNLOAD_WIFI
import one.mixin.android.Constants.Download.MOBILE_DEFAULT
import one.mixin.android.Constants.Download.ROAMING_DEFAULT
import one.mixin.android.Constants.Download.WIFI_DEFAULT
import one.mixin.android.MixinApplication
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.PropertyDao
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.nowInUtc
import one.mixin.android.job.ClearFts4Job.Companion.FTS_CLEAR
import one.mixin.android.job.MigratedFts4Job.Companion.FTS_NEED_MIGRATED_LAST_ROW_ID
import one.mixin.android.vo.Property

object PropertyHelper {
    private const val PREF_PROPERTY_MIGRATED = "pref_property_migrated"

    suspend fun checkAttachmentMigrated(action: () -> Unit) {
        val value = findValueByKey(PREF_MIGRATION_ATTACHMENT, false)
        if (value) {
            action.invoke()
        }
    }

    suspend fun checkTranscriptAttachmentMigrated(action: () -> Unit) {
        val value = findValueByKey(PREF_MIGRATION_TRANSCRIPT_ATTACHMENT, false)
        if (value) {
            action.invoke()
        }
    }

    suspend fun checkTranscriptAttachmentUpdated(action: () -> Unit) {
        val value = findValueByKey(PREF_MIGRATION_TRANSCRIPT_ATTACHMENT_LAST, 0L)
        if (value > 0) {
            action.invoke()
        }
    }

    suspend fun checkCleanupThumb(action: () -> Unit) {
        val value = findValueByKey(PREF_CLEANUP_THUMB, true)
        if (value) {
            action.invoke()
        }
    }

    suspend fun checkCleanupQuoteContent(action: () -> Unit) {
        val value = findValueByKey(PREF_CLEANUP_QUOTE_CONTENT, true)
        if (value) {
            action.invoke()
        }
    }

    suspend fun checkBackupMigrated(action: () -> Unit) {
        val backupMigrated = findValueByKey(PREF_MIGRATION_BACKUP, true)
        if (!backupMigrated) {
            action.invoke()
        }
    }

    suspend fun checkInscriptionMigrated(action: () -> Unit) {
        val value = findValueByKey(PREF_MIGRATION_INSCRIPTION, false)
        if (!value) {
            action.invoke()
        }
    }

    suspend fun checkInscriptionCollectionMigrated(action: () -> Unit) {
        val value = findValueByKey(PREF_MIGRATION_COLLECTION, false)
        if (!value) {
            action.invoke()
        }
    }

    suspend fun checkFtsMigrated(action: () -> Unit) {
        val lastRowId = findValueByKey(FTS_NEED_MIGRATED_LAST_ROW_ID, 0L)
        if (lastRowId > -1) {
            action.invoke()
        }
    }

    suspend fun checkFtsClear(action: () -> Unit) {
        val reduce = findValueByKey(FTS_CLEAR, false)
        if (reduce) {
            action.invoke()
        }
    }

    suspend fun checkMigrated(): PropertyDao {
        val propertyDao = MixinDatabase.getDatabase(MixinApplication.appContext).propertyDao()
        val messageDao = MixinDatabase.getDatabase(MixinApplication.appContext).messageDao()
        if (!hasMigrated(propertyDao)) {
            migrateProperties(propertyDao)
        }
        return propertyDao
    }

    suspend fun updateKeyValue(
        key: String,
        value: String,
    ) {
        val propertyDao = MixinDatabase.getDatabase(MixinApplication.appContext).propertyDao()
        propertyDao.insertSuspend(Property(key, value, nowInUtc()))
    }

    suspend fun updateKeyValue(
        key: String,
        value: Long,
    ) {
        updateKeyValue(key, value.toString())
    }

    suspend fun updateKeyValue(
        key: String,
        value: Int,
    ) {
        updateKeyValue(key, value.toString())
    }

    suspend fun updateKeyValue(
        key: String,
        value: Boolean,
    ) {
        updateKeyValue(key, value.toString())
    }

    suspend fun deleteKeyValue(key: String) {
        val propertyDao = MixinDatabase.getDatabase(MixinApplication.appContext).propertyDao()
        propertyDao.deletePropertyByKey(key)
    }

    suspend fun <T> findValueByKey(
        key: String,
        default: T,
    ): T {
        val propertyDao = MixinDatabase.getDatabase(MixinApplication.appContext).propertyDao()
        val value = propertyDao.findValueByKey(key) ?: return default
        return try {
            when (default) {
                is Int -> {
                    value.toIntOrNull() ?: default
                }
                is Long -> {
                    value.toIntOrNull() ?: default
                }
                is Boolean -> {
                    value.toBooleanStrictOrNull() ?: default
                }
                else -> {
                    value
                }
            } as T
        } catch (e: Exception) {
            default
        }
    }

    private suspend fun migrateProperties(propertyDao: PropertyDao) {
        val pref = MixinApplication.appContext.defaultSharedPreferences
        val updatedAt = nowInUtc()

        val backup = pref.getBoolean(PREF_BACKUP, false)
        // Backup files need to be migrated
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            propertyDao.insertSuspend(Property(PREF_MIGRATION_BACKUP, backup.toString(), updatedAt))
        }
        val period = pref.getInt(BACKUP_PERIOD, 0)
        propertyDao.insertSuspend(Property(BACKUP_PERIOD, period.toString(), updatedAt))
        val lastTime = pref.getLong(BACKUP_LAST_TIME, System.currentTimeMillis())
        propertyDao.insertSuspend(Property(BACKUP_LAST_TIME, lastTime.toString(), updatedAt))

        val duplicateTransfer = pref.getBoolean(PREF_DUPLICATE_TRANSFER, true)
        propertyDao.insertSuspend(Property(PREF_DUPLICATE_TRANSFER, duplicateTransfer.toString(), updatedAt))
        val strangerTransfer = pref.getBoolean(PREF_STRANGER_TRANSFER, true)
        propertyDao.insertSuspend(Property(PREF_STRANGER_TRANSFER, strangerTransfer.toString(), updatedAt))

        val autoWifi = pref.getInt(AUTO_DOWNLOAD_WIFI, WIFI_DEFAULT)
        propertyDao.insertSuspend(Property(AUTO_DOWNLOAD_WIFI, autoWifi.toString(), updatedAt))
        val autoMobile = pref.getInt(AUTO_DOWNLOAD_MOBILE, MOBILE_DEFAULT)
        propertyDao.insertSuspend(Property(AUTO_DOWNLOAD_MOBILE, autoMobile.toString(), updatedAt))
        val autoRoaming = pref.getInt(AUTO_DOWNLOAD_ROAMING, ROAMING_DEFAULT)
        propertyDao.insertSuspend(Property(AUTO_DOWNLOAD_ROAMING, autoRoaming.toString(), updatedAt))

        propertyDao.insertSuspend(Property(PREF_PROPERTY_MIGRATED, true.toString(), updatedAt))
        // Attachment files need to be migrated
        migration()
    }

    suspend fun migration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val messageDao = MixinDatabase.getDatabase(MixinApplication.appContext).messageDao()
            val propertyDao = MixinDatabase.getDatabase(MixinApplication.appContext).propertyDao()
            val transcriptDao = MixinDatabase.getDatabase(MixinApplication.appContext).transcriptDao()
            val updatedAt = nowInUtc()
            propertyDao.insertSuspend(Property(PREF_MIGRATION_ATTACHMENT, (messageDao.hasDoneAttachment()).toString(), updatedAt))
            val lastDoneAttachmentId = transcriptDao.lastDoneAttachmentId() ?: 0
            if (lastDoneAttachmentId > 0) {
                propertyDao.insertSuspend(Property(PREF_MIGRATION_TRANSCRIPT_ATTACHMENT, true.toString(), updatedAt))
                propertyDao.insertSuspend(Property(PREF_MIGRATION_TRANSCRIPT_ATTACHMENT_LAST, lastDoneAttachmentId.toString(), updatedAt))
            }
            propertyDao.insertSuspend(Property(PREF_MIGRATION_BACKUP, true.toString(), updatedAt))
        }
    }

    private suspend fun hasMigrated(propertyDao: PropertyDao) =
        propertyDao.findValueByKey(PREF_PROPERTY_MIGRATED)?.toBooleanStrictOrNull() == true
}
