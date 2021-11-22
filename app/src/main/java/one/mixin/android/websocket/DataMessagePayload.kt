package one.mixin.android.websocket

import androidx.core.net.toUri
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter
import one.mixin.android.util.Attachment

@JsonClass(generateAdapter = true)
data class DataMessagePayload(
    val url: String,
    @Json(name = "file_name")
    val filename: String,
    @Json(name = "mime_type")
    val mimeType: String,
    @Json(name = "file_size")
    val fileSize: Long,
    @Json(name = "attachment_extra")
    val attachmentExtra: String? = null,
) {
    fun toAttachment() = Attachment(url.toUri(), filename, mimeType, fileSize)
}

fun DataMessagePayload.toJson(): String =
    getTypeAdapter<DataMessagePayload>(DataMessagePayload::class.java).toJson(this)
