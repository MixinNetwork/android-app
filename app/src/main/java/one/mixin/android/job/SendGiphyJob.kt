package one.mixin.android.job

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.birbit.android.jobqueue.Params
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import one.mixin.android.MixinApplication
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.extension.copy
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.createGifTemp
import one.mixin.android.extension.encodeBlurHash
import one.mixin.android.extension.getImagePath
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.createMediaMessage
import one.mixin.android.widget.gallery.MimeType
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
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

    @OptIn(ExperimentalCoilApi::class)
    override fun onRun() = runBlocking {
        val ctx = MixinApplication.appContext
        val request = ImageRequest.Builder(ctx)
            .data(url)
            .target {
                ctx.imageLoader.diskCache?.openSnapshot(url)?.use { snapshot ->
                    val imageFile = snapshot.data.toFile()
                    if (imageFile.length() > 0) {
                        sendMessage(ctx, imageFile)
                    }
                }
            }
            .build()
        ctx.imageLoader.enqueue(request)
        return@runBlocking
    }

    private fun sendMessage(ctx: Context, imageFile: File) = runBlocking(Dispatchers.IO) {
        val file = ctx.getImagePath().createGifTemp(conversationId, messageId)
        imageFile.copy(file)
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
