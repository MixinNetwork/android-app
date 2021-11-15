package one.mixin.android.util

import android.os.Build
import one.mixin.android.Constants.Account.Migration.PREF_MIGRATION_ATTACHMENT
import one.mixin.android.Constants.Account.Migration.PREF_MIGRATION_BACKUP
import one.mixin.android.Constants.Account.Migration.PREF_MIGRATION_TRANSCRIPT_ATTACHMENT
import one.mixin.android.Constants.Account.Migration.PREF_MIGRATION_TRANSCRIPT_ATTACHMENT_LAST
import one.mixin.android.Constants.Account.PREF_BACKUP
import one.mixin.android.Constants.Account.PREF_DUPLICATE_TRANSFER
import one.mixin.android.Constants.Account.PREF_FTS4_UPGRADE
import one.mixin.android.Constants.Account.PREF_STRANGER_TRANSFER
import one.mixin.android.Constants.Account.PREF_SYNC_FTS4_OFFSET
import one.mixin.android.Constants.BackUp.BACKUP_LAST_TIME
import one.mixin.android.Constants.BackUp.BACKUP_PERIOD
import one.mixin.android.Constants.Download.AUTO_DOWNLOAD_MOBILE
import one.mixin.android.Constants.Download.AUTO_DOWNLOAD_ROAMING
import one.mixin.android.Constants.Download.AUTO_DOWNLOAD_WIFI
import one.mixin.android.Constants.Download.MOBILE_DEFAULT
import one.mixin.android.Constants.Download.ROAMING_DEFAULT
import one.mixin.android.Constants.Download.WIFI_DEFAULT
import one.mixin.android.MixinApplication
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.PropertyDao
import one.mixin.android.db.TranscriptMessageDao
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.nowInUtc
import one.mixin.android.vo.Property

object PropertyHelper {

    private const val PREF_PROPERTY_MIGRATED = "pref_property_migrated"

    suspend fun checkFts4Upgrade(): Boolean {
        val propertyDao = checkMigrated()
        return propertyDao.findValueByKey(PREF_FTS4_UPGRADE) != true.toString()
    }

    suspend fun checkAttachmentMigrated(action: () -> Unit) {
        val propertyDao = checkMigrated()
        val value = propertyDao.findValueByKey(PREF_MIGRATION_ATTACHMENT)?.toBoolean() ?: false
        if (value) {
            action.invoke()
        }
    }

    suspend fun checkTranscriptAttachmentMigrated(action: () -> Unit) {
        val propertyDao = checkMigrated()
        val value = propertyDao.findValueByKey(PREF_MIGRATION_TRANSCRIPT_ATTACHMENT)?.toBoolean() ?: false
        if (value) {
            action.invoke()
        }
    }

    suspend fun checkTranscriptAttachmentUpdated(action: () -> Unit) {
        val propertyDao = checkMigrated()
        val value = propertyDao.findValueByKey(PREF_MIGRATION_TRANSCRIPT_ATTACHMENT_LAST)?.toLong() ?: 0
        if (value > 0) {
            action.invoke()
        }
    }

    suspend fun checkBackupMigrated(action: () -> Unit) {
        checkWithKey(PREF_MIGRATION_BACKUP, true.toString(), action)
    }

    suspend fun checkMigrated(): PropertyDao {
        val propertyDao = MixinDatabase.getDatabase(MixinApplication.appContext).propertyDao()
        val messageDao = MixinDatabase.getDatabase(MixinApplication.appContext).messageDao()
        val transcriptDao = MixinDatabase.getDatabase(MixinApplication.appContext).transcriptDao()
        if (!hasMigrated(propertyDao)) {
            migrateProperties(propertyDao, messageDao, transcriptDao)
        }
        return propertyDao
    }

    suspend fun updateKeyValue(key: String, value: String) {
        val propertyDao = MixinDatabase.getDatabase(MixinApplication.appContext).propertyDao()
        propertyDao.insertSuspend(Property(key, value, nowInUtc()))
    }

    suspend fun findValueByKey(key: String): String? {
        val propertyDao = MixinDatabase.getDatabase(MixinApplication.appContext).propertyDao()
        return propertyDao.findValueByKey(key)
    }

    private suspend fun checkWithKey(key: String, expectValue: String, action: () -> Unit) {
        val propertyDao = checkMigrated()

        val value = propertyDao.findValueByKey(key)
        if (value != expectValue) {
            action.invoke()
        }
    }

    private suspend fun migrateProperties(propertyDao: PropertyDao, messageDao: MessageDao, transcriptDao: TranscriptMessageDao) {
        val pref = MixinApplication.appContext.defaultSharedPreferences
        val updatedAt = nowInUtc()
        val fts4Upgrade = pref.getBoolean(PREF_FTS4_UPGRADE, messageDao.hasMessage() == null)
        propertyDao.insertSuspend(Property(PREF_FTS4_UPGRADE, fts4Upgrade.toString(), updatedAt))
        val syncFtsOffset = pref.getInt(PREF_SYNC_FTS4_OFFSET, 0)
        propertyDao.insertSuspend(Property(PREF_SYNC_FTS4_OFFSET, syncFtsOffset.toString(), updatedAt))

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
        propertyDao.findValueByKey(PREF_PROPERTY_MIGRATED)?.toBoolean() == true
}
