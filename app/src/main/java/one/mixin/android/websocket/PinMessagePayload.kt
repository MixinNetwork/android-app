package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.util.MoshiHelper.getTypeAdapter

@JsonClass(generateAdapter = true)
data class PinMessagePayload(
    @SerializedName("action")
    @Json(name = "action")
    val action: String,
    @SerializedName("message_ids")
    @Json(name = "message_ids")
    val messageIds: List<String>
)

fun PinMessagePayload.toJson(): String = getTypeAdapter<PinMessagePayload>(PinMessagePayload::class.java).toJson(this)

enum class PinAction {
    PIN,
    UNPIN
}
