package one.mixin.android.job

import android.net.Uri
import com.birbit.android.jobqueue.Params
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.vo.isAttachment
import one.mixin.android.vo.isTranscript
import java.io.File

class TranscriptDeleteJob(private val messageIds: List<String>) : BaseJob(Params(PRIORITY_BACKGROUND).addTags(GROUP).groupBy("transcript_delete").persist()) {

    private val TAG = TranscriptDeleteJob::class.java.simpleName

    companion object {
        const val GROUP = "TranscriptDeleteJob"
        private const val serialVersionUID = 1L
    }

    override fun onRun() {
        messageIds.forEach { messageId ->
            messageDao.deleteMessage(messageId)
            transcriptMessageDao.getTranscript(messageId).forEach { transcriptMessage ->
                if (transcriptMessage.isAttachment()) {
                    File(Uri.parse(transcriptMessage.mediaUrl).path!!).deleteOnExit()
                    transcriptMessageDao.delete(transcriptMessage)
                } else if (transcriptMessage.isTranscript()) {
                    deleteTranscript(transcriptMessage)
                }
            }
        }
    }

    private fun deleteTranscript(transcriptMessage: TranscriptMessage) {
        val list = transcriptMessageDao.getTranscript(transcriptMessage.messageId)
        if (list.isEmpty()) {
            return
        } else {
            list.forEach { t ->
                if (t.isTranscript()) {
                    deleteTranscript(transcriptMessage)
                } else {
                    val count = transcriptMessageDao.countTranscriptByMessageId(t.messageId)
                    if (count > 1) {
                        return@forEach
                    } else {
                        if (t.isAttachment()) {
                            File(Uri.parse(transcriptMessage.mediaUrl).path!!).deleteOnExit()
                        }
                        transcriptMessageDao.delete(t)
                    }
                }
            }
        }
    }
}
