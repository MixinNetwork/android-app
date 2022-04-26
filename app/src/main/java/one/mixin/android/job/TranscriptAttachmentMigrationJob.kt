package one.mixin.android.job

import android.os.Build
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.extension.getMediaPath
import one.mixin.android.extension.getTranscriptDirPath
import one.mixin.android.extension.hasWritePermission
import one.mixin.android.util.reportException
import timber.log.Timber
import java.io.IOException
import java.nio.file.Files

class TranscriptAttachmentMigrationJob : BaseJob(Params(PRIORITY_LOWER).groupBy(GROUP_ID).persist()) {
    companion object {
        private const val GROUP_ID = "transcript_attachment_migration"
        private const val serialVersionUID = -30L
    }

    override fun onRun() = runBlocking {
        if (!hasWritePermission()) return@runBlocking
        val oldDir = MixinApplication.get().applicationContext.getTranscriptDirPath(true)
        if (oldDir.exists()) {
            val newDir = MixinApplication.get().applicationContext.getTranscriptDirPath(false)
            if (newDir.parentFile?.exists() != true) {
                newDir.parentFile?.mkdirs()
            }
            if (newDir.exists()) {
                newDir.deleteRecursively()
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Files.move(oldDir.toPath(), newDir.toPath())
                } else {
                    oldDir.renameTo(newDir)
                }
            } catch (e: IOException) {
                Timber.e("Attachment migration ${e.message}")
                reportException(e)
            }
            Timber.e("Transcript attachment migration ${oldDir.absolutePath} ${newDir.absolutePath}")
        } else {
            Timber.e("Transcript attachment migration old not exists")
        }

        if (propertyDao.findValueByKey(Constants.Account.Migration.PREF_MIGRATION_ATTACHMENT)?.toBoolean() != true) {
            MixinApplication.appContext.getMediaPath(true)?.deleteRecursively()
        }
        propertyDao.updateValueByKey(Constants.Account.Migration.PREF_MIGRATION_TRANSCRIPT_ATTACHMENT, false.toString())
        Timber.e("Transcript attachment migration completed!!!")
    }
}
