package one.mixin.android.job

import android.net.Uri
import androidx.core.net.toUri
import com.birbit.android.jobqueue.Params
import com.bugsnag.android.Bugsnag
import one.mixin.android.MixinApplication
import one.mixin.android.extension.copy
import one.mixin.android.extension.getExtensionName
import one.mixin.android.extension.getTranscriptFile
import one.mixin.android.extension.joinWhiteSpace
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.MessageFts4Helper
import one.mixin.android.vo.*
import java.io.File

class SendTranscriptJob(
    val message: Message,
    val transcriptMessages: List<TranscriptMessage>,
    messagePriority: Int = PRIORITY_SEND_MESSAGE
) : MixinJob(Params(messagePriority).groupBy("send_message_group").persist(), message.id) {

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun onAdded() {
        super.onAdded()
        if (chatWebSocket.connected) {
            jobManager.start()
        }
        val conversation = conversationDao.findConversationById(message.conversationId)
        if (conversation != null) {
            messageDao.insert(message)
            transcriptMessages.forEach { transcript ->
                if (transcript.isAttachment()) {
                    val file = File(Uri.parse(transcript.mediaUrl).path)
                    if (file.exists()) {
                        val outFile = MixinApplication.appContext.getTranscriptFile(
                            message.conversationId,
                            file.nameWithoutExtension,
                            file.name.getExtensionName().notNullWithElse({ ".$it" }, ""),
                            transcript.transcriptId,
                        )
                        file.copy(outFile)
                        transcript.mediaUrl = outFile.toUri().toString()
                        transcript.mediaStatus = MediaStatus.CANCELED.name
                    } else {
                        transcript.mediaUrl = null
                        transcript.mediaStatus = MediaStatus.DONE.name
                    }
                } else if (transcript.isTranscript()) {
                    transcriptMessageDao.getTranscript(transcript.messageId).forEach {

                    }
                }
            }
            transcriptMessageDao.insertList(transcriptMessages)
        } else {
            Bugsnag.notify(Throwable("Insert failed, no conversation exist"))
        }
    }

    override fun cancel() {
    }

    override fun onRun() {
        val transcripts = getTranscripts(message.id, mutableListOf())
        val stringBuffer = StringBuffer()
        transcripts.filter { it.isText() || it.isPost() || it.isData() || it.isContact() }.forEach { transcript ->
            if (transcript.isData()) {
                transcript.mediaName
            } else if (transcript.isContact()) {
                transcript.sharedUserId?.let { userId -> userDao.findUser(userId) }?.fullName
            } else {
                transcript.content
            }?.joinWhiteSpace()?.let {
                stringBuffer.append(it)
            }
        }
        MessageFts4Helper.insertMessageFts4(message.id, stringBuffer.toString())
        if (transcripts.any { t -> t.isAttachment() }) {
            messageDao.insert(message)
            transcripts.forEach { t ->
                jobManager.addJob(SendTranscriptAttachmentMessageJob(t, message.isPlain()))
            }
        } else {
            message.mediaStatus = MediaStatus.DONE.name
            messageDao.insert(message)
            message.content = GsonHelper.customGson.toJson(transcripts)
            jobManager.addJob(SendMessageJob(message))
        }
    }

    private fun getTranscripts(transcriptId: String, list: MutableList<TranscriptMessage>): MutableList<TranscriptMessage> {
        val transcripts = transcriptMessageDao.getTranscript(transcriptId)
        list.addAll(transcripts)
        transcripts.asSequence().filter { t -> t.isTranscript() }.forEach { transcriptMessage ->
            getTranscripts(transcriptMessage.messageId, list)
        }
        return list
    }
}