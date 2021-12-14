package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class BlazeSignalKeyMessage(
    @SerializedName("message_id")
    val message_id: String,
    @SerializedName("recipient_id")
    val recipient_id: String,
    @SerializedName("data")
    val data: String,
    @SerializedName("session_id")
    val sessionId: String? = null
)

fun createBlazeSignalKeyMessage(recipientId: String, data: String, sessionId: String? = null) =
    BlazeSignalKeyMessage(UUID.randomUUID().toString(), recipientId, data, sessionId)
