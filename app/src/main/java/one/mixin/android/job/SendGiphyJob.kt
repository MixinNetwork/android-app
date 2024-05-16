package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import com.bumptech.glide.Glide
import one.mixin.android.MixinApplication
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.createGifTemp
import one.mixin.android.extension.encodeBlurHash
import one.mixin.android.extension.getImagePath
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.createMediaMessage
import one.mixin.android.widget.gallery.MimeType
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

class SendGiphyJob(
    private val conversationId: String,
    private val senderId: String,
    private val url: String,
    private val width: Int,
    private val height: Int,
    private val size: Long,
    private val category: String,
    private val messageId: String,
    private val previewUrl: String,
    private val time: String,
) : BaseJob(Params(PRIORITY_BACKGROUND).addTags(TAG)) {
    companion object {
        private const val serialVersionUID = 1L
        const val TAG = "SendGiphyJob"
    }

    override fun onAdded() {
        val message =
            createMediaMessage(
                messageId,
                conversationId,
                senderId,
                category,
                null,
                url,
                MimeType.GIF.toString(),
                size,
                width,
                height,
                previewUrl,
                null,
                null,
                time,
                MediaStatus.PENDING,
                MessageStatus.SENDING.name,
            )
        conversationDao.updateLastMessageId(message.messageId, message.createdAt, message.conversationId)
        // Todo check
        MessageFlow.update(message.conversationId, message.messageId)
    }

    override fun onRun() {
        val ctx = MixinApplication.appContext
        val f = Glide.with(ctx).downloadOnly().load(url).submit().get(10, TimeUnit.SECONDS)
        val file = ctx.getImagePath().createGifTemp(conversationId, messageId)
        file.copyFromInputStream(FileInputStream(f))
        val thumbnail = file.encodeBlurHash()
        val mediaSize = file.length()
        val message =
            createMediaMessage(
                messageId,
                conversationId,
                senderId,
                category,
                null,
                file.name,
                MimeType.GIF.toString(),
                mediaSize,
                width,
                height,
                thumbnail,
                null,
                null,
                time,
                MediaStatus.PENDING,
                MessageStatus.SENDING.name,
            )
        messageDao.updateGiphyMessage(messageId, file.name, mediaSize, thumbnail)
        MessageFlow.update(message.conversationId, message.messageId)
        jobManager.addJobInBackground(SendAttachmentMessageJob(message))
    }
}
