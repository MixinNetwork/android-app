package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.PREF_BACKUP
import one.mixin.android.MixinApplication
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getBackupPath
import one.mixin.android.extension.hasWritePermission
import one.mixin.android.extension.putBoolean
import one.mixin.android.util.backup.findOldBackupSync
import timber.log.Timber
import java.io.File

class BackupMigrationJob : BaseJob(Params(PRIORITY_LOWER).groupBy(GROUP_ID).persist()) {
    companion object {
        private const val GROUP_ID = "backup_migration"
    }

    override fun onRun() {
        val startTime = System.currentTimeMillis()
        val preferences = MixinApplication.appContext.defaultSharedPreferences
        if (!hasWritePermission()) return
        if (!preferences.getBoolean(PREF_BACKUP, false)) {
            val oldBackup = findOldBackupSync(MixinApplication.appContext)
            if (oldBackup != null && oldBackup.exists()) {
                MixinApplication.get().getBackupPath(true)?.let { backupDir ->
                    oldBackup.renameTo(File("$backupDir${File.separator}${Constants.DataBase.DB_NAME}"))
                }
            }
            preferences.putBoolean(PREF_BACKUP, true)
        }
        Timber.d("Backup migration cost: ${System.currentTimeMillis() - startTime} ms")
    }
}
