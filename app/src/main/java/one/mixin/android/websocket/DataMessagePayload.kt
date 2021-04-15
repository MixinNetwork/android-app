package one.mixin.android.websocket

import androidx.core.net.toUri
import com.google.gson.annotations.SerializedName
import one.mixin.android.util.Attachment

data class DataMessagePayload(
    val url: String,
    @SerializedName("file_name")
    val filename: String,
    @SerializedName("mime_type")
    val mimeType: String,
    @SerializedName("file_size")
    val fileSize: Long,
    @SerializedName("attachment_message_payload")
    val attachmentMessagePayload: String? = null,
) {
    fun toAttachment() = Attachment(url.toUri(), filename, mimeType, fileSize, attachmentMessagePayload)
}
