package one.mixin.android.repository

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import one.mixin.android.api.service.ConversationService
import one.mixin.android.crypto.Util
import one.mixin.android.extension.toPNGBytes
import java.io.IOException
import timber.log.Timber
import javax.inject.Inject

class ShareCoverRepository @Inject constructor(
    private val conversationService: ConversationService,
) {
    suspend fun upload(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val response = conversationService.requestAttachmentSuspend()
        val attachment = response.data?.takeIf { response.isSuccess }
            ?: throw IOException("Unable to request share cover attachment")
        val uploadUrl = attachment.upload_url?.takeIf { it.isNotBlank() }
            ?: throw IOException("Missing share cover upload URL")
        val viewUrl = attachment.view_url?.takeIf { it.isNotBlank() }
            ?: throw IOException("Missing share cover view URL")
        val bytes = bitmap.toPNGBytes()
        try {
            bytes.inputStream().use { input ->
                Util.uploadAttachment(uploadUrl, input, bytes.size.toLong(), null, { _, _ -> }) {
                    !isActive
                }
            }
        } catch (e: IOException) {
            Timber.w("Share cover PUT failed (%s)", e.javaClass.simpleName)
            throw e
        }
        ensureActive()
        viewUrl
    }
}
