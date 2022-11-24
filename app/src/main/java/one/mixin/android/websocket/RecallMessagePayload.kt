package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName

data class RecallMessagePayload(
    @SerializedName("message_id")
    val messageId: String,
)
