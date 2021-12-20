package one.mixin.android.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter

@JsonClass(generateAdapter = true)
data class PinMessagePayload(
    @Json(name = "action")
    val action: String,
    @Json(name = "message_ids")
    val messageIds: List<String>
)

fun PinMessagePayload.toJson(): String = getTypeAdapter<PinMessagePayload>(PinMessagePayload::class.java).toJson(this)

enum class PinAction {
    PIN,
    UNPIN
}
