package one.mixin.android.job

import android.os.Build
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.extension.getTranscriptDirPath
import one.mixin.android.extension.hasWritePermission
import java.nio.file.Files

class TranscriptAttachmentMigrationJob : BaseJob(Params(PRIORITY_LOWER).groupBy(GROUP_ID).persist()) {
    companion object {
        private const val GROUP_ID = "transcript_attachment_migration"
    }

    override fun onRun() = runBlocking {
        if (!hasWritePermission()) return@runBlocking
        val oldDir = MixinApplication.get().applicationContext.getTranscriptDirPath(true)
        val newDir = MixinApplication.get().applicationContext.getTranscriptDirPath(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Files.move(oldDir.toPath(), newDir.toPath())
        } else {
            oldDir.renameTo(newDir)
        }
        propertyDao.updateValueByKey(Constants.Account.Migration.PREF_MIGRATION_TRANSCRIPT_ATTACHMENT, false.toString())
    }
}
