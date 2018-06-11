package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName

class TransferAttachmentData(
    @SerializedName("key")
    var key: ByteArray?,
    @SerializedName("digest")
    var digest: ByteArray?,
    @SerializedName("attachment_id")
    var attachmentId: String,
    @SerializedName("mine_type")
    var mineType: String,
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
    var waveform: ByteArray? = null
)

fun TransferAttachmentData.invalidData(): Boolean {
    if (width == null || width == 0 || height == null || height == 0) {
        return true
    }
    return false
}