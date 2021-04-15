package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName
import one.mixin.android.crypto.Base64
import one.mixin.android.util.GsonHelper

data class AttachmentMessagePayload(
    @SerializedName("key")
    var key: ByteArray?,
    @SerializedName("digest")
    var digest: ByteArray?,
    @SerializedName("attachment_id")
    var attachmentId: String,
    @SerializedName("mime_type")
    var mimeType: String,
    @SerializedName("size")
    var size: Long,
    @SerializedName("name")
    var name: String?,
    @SerializedName("width")
    var width: Int?,
    @SerializedName("height")
    var height: Int?,
    @SerializedName("thumbnail")
    var thumbnail: String?,
    @SerializedName("duration")
    var duration: Long? = null,
    @SerializedName("waveform")
    var waveform: ByteArray? = null,
    @SerializedName("caption")
    var caption: String? = null,
    @SerializedName("created_at")
    var createdAt: String? = null,
)

fun AttachmentMessagePayload.invalidData(): Boolean {
    if (width == null || width == 0 || height == null || height == 0) {
        return true
    }
    return false
}

fun String.toAttachmentMessagePayload(): AttachmentMessagePayload? =
    GsonHelper.customGson.fromJson(String(Base64.decode(this)), AttachmentMessagePayload::class.java)
