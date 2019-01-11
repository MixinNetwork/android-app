package one.mixin.android.job

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.text.format.DateUtils.DAY_IN_MILLIS
import android.text.format.DateUtils.WEEK_IN_MILLIS
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.birbit.android.jobqueue.Params
import one.mixin.android.Constants.BackUp.BACKUP_LAST_TIME
import one.mixin.android.Constants.BackUp.BACKUP_PERIOD
import one.mixin.android.MixinApplication
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getCacheMediaPath
import one.mixin.android.extension.getMediaPath
import one.mixin.android.extension.moveChileFileToDir
import one.mixin.android.extension.putLong
import one.mixin.android.util.backup.BackupLiveData
import one.mixin.android.util.backup.BackupNotification
import one.mixin.android.util.backup.Result
import java.io.File

class BackupJob(private val force: Boolean = false) : BaseJob(Params(if (force) {
    PRIORITY_UI_HIGH
} else {
    PRIORITY_BACKGROUND
}).addTags(GROUP).requireNetwork().persist()) {

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "BackupJob"
        val backupLiveData = BackupLiveData()
    }

    override fun onRun() {
        val context = MixinApplication.appContext
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        if (force) {
            backup(context)
        } else {
            val option = context.defaultSharedPreferences.getInt(BACKUP_PERIOD, 0)
            if (option in 1..3) {
                val currentTime = System.currentTimeMillis()
                val lastTime = context.defaultSharedPreferences.getLong(BACKUP_LAST_TIME, currentTime)
                val timeDiff = currentTime - lastTime
                if (timeDiff >= when (option) {
                        1 -> DAY_IN_MILLIS
                        2 -> WEEK_IN_MILLIS
                        3 -> DAY_IN_MILLIS * 30
                        else -> Long.MAX_VALUE
                    }) {
                    backup(context)
                }
            }
        }
    }

    private fun cleanMedia() {
        val mediaCachePath = MixinApplication.appContext.getCacheMediaPath()
        val mediaPath = MixinApplication.appContext.getMediaPath().absolutePath
        if (!mediaCachePath.exists()) {
            return
        }
        for (mediaCacheChild in mediaCachePath.listFiles()) {
            if (mediaCacheChild.isDirectory) {
                val local = File("$mediaPath${File.separator}${mediaCacheChild.name}${File.separator}")
                mediaCacheChild.moveChileFileToDir(local) { newFile, oldFile ->
                    messageDao.updateMediaUrl(newFile.toUri().toString(), oldFile.toUri().toString())
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private fun backup(context: Context) {
        backupLiveData.start()
        BackupNotification.show()
        cleanMedia()
        one.mixin.android.util.backup.backup(context) { result ->
            if (result == Result.SUCCESS) {
                backupLiveData.setResult(false, result)
                BackupNotification.cancel()
                context.defaultSharedPreferences.putLong(BACKUP_LAST_TIME, System.currentTimeMillis())
            } else {
                backupLiveData.setResult(false, result)
                BackupNotification.cancel()
            }
        }
    }
}