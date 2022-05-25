package one.mixin.android.job

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateUtils.DAY_IN_MILLIS
import android.text.format.DateUtils.WEEK_IN_MILLIS
import androidx.annotation.RequiresPermission
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.PREF_BACKUP
import one.mixin.android.Constants.BackUp.BACKUP_LAST_TIME
import one.mixin.android.Constants.BackUp.BACKUP_PERIOD
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.toast
import one.mixin.android.util.PropertyHelper
import one.mixin.android.util.backup.BackupLiveData
import one.mixin.android.util.backup.BackupNotification
import one.mixin.android.util.backup.Result
import one.mixin.android.util.backup.backup
import one.mixin.android.util.backup.backupApi29
import one.mixin.android.util.backup.findOldBackupSync
import timber.log.Timber

class BackupJob(private val force: Boolean = false, private val delete: Boolean = false, private val backupMedia: Boolean = true) : BaseJob(
    Params(
        if (force) {
            PRIORITY_UI_HIGH
        } else {
            PRIORITY_BACKGROUND
        }
    ).addTags(GROUP).setSingleId(GROUP).persist()
) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "BackupJob"
        val backupLiveData = BackupLiveData()
    }

    override fun onRun() = runBlocking {
        val context = MixinApplication.appContext
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return@runBlocking
        }
        if (force) {
            internalBackup(context)
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && propertyDao.findValueByKey(PREF_BACKUP)?.toBooleanStrictOrNull() == true) {
            val option = PropertyHelper.findValueByKey(BACKUP_PERIOD)?.toIntOrNull() ?: 0
            if (option in 1..3) {
                val currentTime = System.currentTimeMillis()
                val lastTime = PropertyHelper.findValueByKey(BACKUP_LAST_TIME)?.toLongOrNull() ?: currentTime
                val timeDiff = currentTime - lastTime
                if (timeDiff >= when (option) {
                    1 -> DAY_IN_MILLIS
                    2 -> WEEK_IN_MILLIS
                    3 -> DAY_IN_MILLIS * 30
                    else -> Long.MAX_VALUE
                }
                ) {
                    internalBackup(context)
                }
            }
        }
        if (delete) {
            findOldBackupSync(MixinApplication.appContext)?.deleteRecursively()
            findOldBackupSync(MixinApplication.appContext, true)?.deleteRecursively()
            propertyDao.updateValueByKey(Constants.Account.Migration.PREF_MIGRATION_BACKUP, false.toString())
        }
    }

    @WorkerThread
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private fun internalBackup(context: Context) = runBlocking {
        try {
            backupLiveData.start()
            BackupNotification.show()
            (
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    backupApi29(context, backupMedia) { result ->
                        backupLiveData.setResult(false, result)
                        BackupNotification.cancel()
                        if (result == Result.SUCCESS) {
                            this.launch {
                                PropertyHelper.updateKeyValue(
                                    BACKUP_LAST_TIME,
                                    System.currentTimeMillis().toString()
                                )
                            }
                            toast(R.string.Backup_success)
                        } else {
                            backupLiveData.setResult(false, null)
                            BackupNotification.cancel()
                            toast(R.string.backup_failure_tip)
                        }
                    }
                } else {
                    backup(context) { result ->
                        backupLiveData.setResult(false, result)
                        BackupNotification.cancel()
                        if (result == Result.SUCCESS) {
                            this.launch {
                                PropertyHelper.updateKeyValue(
                                    BACKUP_LAST_TIME,
                                    System.currentTimeMillis().toString()
                                )
                            }
                            toast(R.string.Backup_success)
                        } else {
                            backupLiveData.setResult(false, null)
                            BackupNotification.cancel()
                            toast(R.string.backup_failure_tip)
                        }
                    }
                }
                )
        } catch (e: Exception) {
            backupLiveData.setResult(false, null)
            BackupNotification.cancel()
            Timber.e(e)
            throw e
        }
    }
}
