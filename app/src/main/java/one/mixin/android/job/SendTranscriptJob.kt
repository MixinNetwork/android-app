package one.mixin.android.job

import android.net.Uri
import com.birbit.android.jobqueue.Params
import one.mixin.android.MixinApplication
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.db.insertMessage
import one.mixin.android.extension.copy
import one.mixin.android.extension.getExtensionName
import one.mixin.android.extension.getTranscriptFile
import one.mixin.android.extension.joinWhiteSpace
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.fts.insertFts4
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.reportException
import one.mixin.android.vo.EncryptCategory
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.Message
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.vo.absolutePath
import one.mixin.android.vo.isAttachment
import one.mixin.android.vo.isContact
import one.mixin.android.vo.isData
import one.mixin.android.vo.isEncrypted
import one.mixin.android.vo.isPost
import one.mixin.android.vo.isSignal
import one.mixin.android.vo.isText
import one.mixin.android.vo.isTranscript
import java.io.File

class SendTranscriptJob(
    val message: Message,
    private val transcriptMessages: List<TranscriptMessage>,
    messagePriority: Int = PRIORITY_SEND_MESSAGE,
) : MixinJob(Params(messagePriority).groupBy("send_message_group").persist(), message.messageId) {
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
            val stringBuffer = StringBuffer()
            transcriptMessages.filter { it.isText() || it.isPost() || it.isData() || it.isContact() }.forEach { transcript ->
                if (transcript.isData()) {
                    transcript.mediaName
                } else {
                    if (transcript.isContact()) {
                        transcript.sharedUserId?.let { userId -> userDao.findUser(userId) }?.fullName
                    } else {
                        transcript.content
                    }?.joinWhiteSpace()?.let {
                        stringBuffer.append(it)
                    }
                }
            }
            ftsDatabase.insertFts4(stringBuffer.toString(), message.conversationId, message.messageId, message.category, message.userId, message.createdAt)
            database.insertMessage(message)
            MessageFlow.insert(message.conversationId, message.messageId)
            transcriptMessages.forEach { transcript ->
                if (transcript.isAttachment()) {
                    val mediaUrl = Uri.parse(transcript.absolutePath())
                    if (mediaUrl == null) {
                        transcript.mediaUrl = null
                        transcript.mediaStatus = MediaStatus.DONE.name
                    } else {
                        val file = File(requireNotNull(Uri.parse(transcript.absolutePath()).path))
                        if (file.exists()) {
                            val outFile =
                                MixinApplication.appContext.getTranscriptFile(
                                    transcript.messageId,
                                    file.name.getExtensionName().notNullWithElse({ ".$it" }, ""),
                                )
                            if (!outFile.exists() || outFile.length() <= 0) {
                                file.copy(outFile)
                            }
                            transcript.mediaUrl = outFile.name
                            transcript.mediaStatus = MediaStatus.CANCELED.name
                        } else {
                            transcript.mediaUrl = null
                            transcript.mediaStatus = MediaStatus.DONE.name
                        }
                    }
                }
            }
            transcriptMessageDao.insertList(transcriptMessages)
        } else {
            reportException(Throwable("Insert failed, no conversation exist"))
        }
    }

    override fun cancel() {
    }

    override fun onRun() {
        val transcripts = mutableSetOf<TranscriptMessage>()
        getTranscripts(message.messageId, transcripts)

        if (transcripts.any { t -> t.isAttachment() }) {
            val mediaSize = transcripts.sumOf { t -> t.mediaSize ?: 0 }
            messageDao.updateMediaSize(mediaSize, message.messageId)
            MessageFlow.update(message.conversationId, message.messageId)
            transcripts.filter { t ->
                t.isAttachment()
            }.forEach { t ->
                val encryptCategory =
                    when {
                        message.isEncrypted() -> EncryptCategory.ENCRYPTED
                        message.isSignal() -> EncryptCategory.SIGNAL
                        else -> EncryptCategory.PLAIN
                    }
                jobManager.addJob(SendTranscriptAttachmentMessageJob(t, encryptCategory, message.messageId))
            }
        } else {
            messageDao.updateMediaStatus(MediaStatus.DONE.name, message.messageId)
            MessageFlow.update(message.conversationId, message.messageId)
            message.mediaStatus = MediaStatus.DONE.name
            message.content = GsonHelper.customGson.toJson(transcripts)
            jobManager.addJob(SendMessageJob(message))
        }
    }

    private fun getTranscripts(
        transcriptId: String,
        list: MutableSet<TranscriptMessage>,
    ) {
        val transcripts = transcriptMessageDao.getTranscript(transcriptId)
        list.addAll(transcripts)
        transcripts.asSequence().filter { t -> t.isTranscript() }.forEach { transcriptMessage ->
            getTranscripts(transcriptMessage.messageId, list)
        }
    }
}
