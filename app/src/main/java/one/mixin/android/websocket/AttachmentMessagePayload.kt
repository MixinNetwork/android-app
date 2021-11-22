package one.mixin.android.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.extension.base64Encode
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter

@JsonClass(generateAdapter = true)
data class AttachmentMessagePayload(
    @Json(name = "key")
    var key: ByteArray?,
    @Json(name = "digest")
    var digest: ByteArray?,
    @Json(name = "attachment_id")
    var attachmentId: String,
    @Json(name ="mime_type")
    var mimeType: String,
    @Json(name ="size")
    var size: Long,
    @Json(name ="name")
    var name: String?,
    @Json(name ="width")
    var width: Int?,
    @Json(name ="height")
    var height: Int?,
    @Json(name ="thumbnail")
    var thumbnail: String?,
    @Json(name ="duration")
    var duration: Long? = null,
    @Json(name ="waveform")
    var waveform: ByteArray? = null,
    @Json(name ="caption")
    var caption: String? = null,
    @Json(name ="created_at")
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
