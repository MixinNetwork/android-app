package one.mixin.android.job

import androidx.core.net.toFile
import androidx.core.net.toUri
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.Account.Migration.PREF_MIGRATION_TRANSCRIPT_ATTACHMENT_LAST
import one.mixin.android.MixinApplication
import one.mixin.android.extension.getTranscriptDirPath
import one.mixin.android.extension.isFileUri
import timber.log.Timber
import java.io.File

class TranscriptAttachmentUpdateJob : BaseJob(Params(PRIORITY_LOWER).groupBy(GROUP_ID).persist()) {
    companion object {
        private var serialVersionUID: Long = 1L
        private const val GROUP_ID = "TranscriptAttachmentUpdateJob"
        private const val EACH = 10
    }

    override fun onRun() =
        runBlocking {
            val lastId = propertyDao.findValueByKey(PREF_MIGRATION_TRANSCRIPT_ATTACHMENT_LAST)?.toLong() ?: return@runBlocking
            val list = transcriptMessageDao.findAttachmentMigration(lastId, EACH)
            list.forEach { attachment ->
                if (attachment.mediaUrl?.isFileUri() == true) {
                    val file = attachment.mediaUrl.toUri().toFile()
                    if (file.exists()) {
                        Timber.d("Transcript attachment update ${attachment.mediaUrl}")
                        transcriptMessageDao.updateMediaUrl(file.name, attachment.messageId)
                    } else {
                        val newFile = File("${MixinApplication.get().getTranscriptDirPath()}${File.separator}${file.name}")
                        if (newFile.exists()) {
                            Timber.d("Transcript attachment update ${newFile.absoluteFile}")
                            transcriptMessageDao.updateMediaUrl(file.name, attachment.messageId)
                        }
                    }
                }
            }
            if (list.size < EACH) {
                Timber.d("Transcript attachment update completed!!!")
                propertyDao.deletePropertyByKey(PREF_MIGRATION_TRANSCRIPT_ATTACHMENT_LAST)
            } else {
                propertyDao.updateValueByKey(PREF_MIGRATION_TRANSCRIPT_ATTACHMENT_LAST, list.last().rowid.toString())
                jobManager.addJobInBackground(TranscriptAttachmentUpdateJob())
            }
        }
}
