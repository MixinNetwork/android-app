package one.mixin.android.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.util.MoshiHelper

@JsonClass(generateAdapter = true)
data class AudioMessagePayload(
    @Json(name = "message_id")
    val messageId: String,
    val url: String,
    val duration: Long,
    @Json(name = "wave_form")
    val waveForm: ByteArray,
    @Json(name = "attachment_extra")
    val attachmentExtra: String? = null,
)

fun AudioMessagePayload.toJson(): String =
    MoshiHelper.getTypeAdapter<AudioMessagePayload>(AudioMessagePayload::class.java).toJson(this)
