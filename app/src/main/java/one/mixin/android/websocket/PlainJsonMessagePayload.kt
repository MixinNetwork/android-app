package one.mixin.android.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class PlainJsonMessagePayload(
    @Json(name = "action")
    val action: String,
    @Json(name = "messages")
    val messages: List<String>? = null,
    @Json(name = "user_id")
    val userId: String? = null,
    @Json(name = "message_id")
    val messageId: String? = null,
    @Json(name = "session_id")
    val session_id: String? = null,
    @Json(name = "ack_messages")
    val ackMessages: List<BlazeAckMessage>? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 7L
    }
}

data class ResendData(
    val userId: String,
    val messageId: String,
    val sessionId: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 4L
    }
}

enum class PlainDataAction { RESEND_KEY, NO_KEY, RESEND_MESSAGES, ACKNOWLEDGE_MESSAGE_RECEIPTS }
