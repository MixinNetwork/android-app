package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import com.bugsnag.android.Bugsnag
import one.mixin.android.vo.*

class SendTranscriptJob(
    val message: Message,
    val transcripts: List<Transcript>,
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
            transcriptDao.insertList(transcripts)
        } else {
            Bugsnag.notify(Throwable("Insert failed, no conversation exist"))
        }
    }

    override fun cancel() {
    }

    override fun onRun() {
        val list = transcripts.filter { it.isAttachment() }
        if (list.isEmpty()) {
            jobManager.addJob(SendMessageJob(message))
        } else {
            messageDao.insert(message)
            list.forEach { t ->
                jobManager.addJob(SendTranscriptAttachmentMessageJob(t, message.isPlain()))
            }
        }
    }
}