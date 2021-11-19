package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.extension.base64Encode
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter

@JsonClass(generateAdapter = true)
data class AttachmentMessagePayload(
    @SerializedName("key")
    @Json(name = "key")
    var key: ByteArray?,
    @SerializedName("digest")
    @Json(name = "digest")
    var digest: ByteArray?,
    @SerializedName("attachment_id")
    @Json(name = "attachment_id")
    var attachmentId: String,
    @SerializedName("mime_type")
    @Json(name = "mime_type")
    var mimeType: String,
    @SerializedName("size")
    @Json(name = "size")
    var size: Long,
    @SerializedName("name")
    @Json(name = "name")
    var name: String?,
    @SerializedName("width")
    @Json(name = "width")
    var width: Int?,
    @SerializedName("height")
    @Json(name = "height")
    var height: Int?,
    @SerializedName("thumbnail")
    @Json(name = "thumbnail")
    var thumbnail: String?,
    @SerializedName("duration")
    @Json(name = "duration")
    var duration: Long? = null,
    @SerializedName("waveform")
    @Json(name = "waveform")
    var waveform: ByteArray? = null,
    @SerializedName("caption")
    @Json(name = "caption")
    var caption: String? = null,
    @SerializedName("created_at")
    @Json(name = "created_at")
    var createdAt: String? = null,
)

fun AttachmentMessagePayload.invalidData(): Boolean {
    if (width == null || width == 0 || height == null || height == 0) {
        return true
    }
    return false
}

fun AttachmentMessagePayload.toJsonBase64() =
    getTypeAdapter<AttachmentMessagePayload>(AttachmentMessagePayload::class.java).toJson(this).base64Encode()
