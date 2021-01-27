package one.mixin.android.job

import android.net.Uri
import com.birbit.android.jobqueue.Params
import com.bumptech.glide.Glide
import one.mixin.android.MixinApplication
import one.mixin.android.extension.bitmap2String
import one.mixin.android.extension.blurThumbnail
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.createGifTemp
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.getImageSize
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
    private val time: String
) : BaseJob(Params(PRIORITY_BACKGROUND).addTags(TAG)) {

    companion object {
        private const val serialVersionUID = 1L
        const val TAG = "SendGiphyJob"
    }

    override fun onAdded() {
        val message = createMediaMessage(
            messageId, conversationId, senderId, category, null, url,
            MimeType.GIF.toString(), size, width, height, previewUrl, null, null,
            time, MediaStatus.PENDING, MessageStatus.SENDING.name
        )
        messageDao.insert(message)
    }

    override fun onRun() {
        val ctx = MixinApplication.appContext
        val f = Glide.with(ctx).downloadOnly().load(url).submit().get(10, TimeUnit.SECONDS)
        val file = ctx.getImagePath().createGifTemp(conversationId, messageId)
        file.copyFromInputStream(FileInputStream(f))
        val size = getImageSize(file)
        val thumbnail = file.blurThumbnail(size)?.bitmap2String()
        val message = createMediaMessage(
            messageId, conversationId, senderId, category, null, Uri.fromFile(file).toString(),
            MimeType.GIF.toString(), file.length(), width, height, thumbnail, null, null,
            time, MediaStatus.PENDING, MessageStatus.SENDING.name
        )
        messageDao.insert(message)
        jobManager.addJobInBackground(SendAttachmentMessageJob(message))
    }
}
