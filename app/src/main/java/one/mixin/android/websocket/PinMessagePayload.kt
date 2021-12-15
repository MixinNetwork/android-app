package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName

data class PinMessagePayload(
    @SerializedName("action")
    val action: String,
    @SerializedName("message_ids")
    val messageIds: List<String>
)

enum class PinAction {
    PIN,
    UNPIN
}
