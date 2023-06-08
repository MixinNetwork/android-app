package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName
import ulid.ULID

data class BlazeSignalKeyMessage(
    @SerializedName("message_id")
    val message_id: String,
    @SerializedName("recipient_id")
    val recipient_id: String,
    @SerializedName("data")
    val data: String,
    @SerializedName("session_id")
    val sessionId: String? = null,
)

fun createBlazeSignalKeyMessage(recipientId: String, data: String, sessionId: String? = null) =
    BlazeSignalKeyMessage(ULID.randomULID(), recipientId, data, sessionId)
