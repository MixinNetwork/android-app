package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class TransferPlainData(
    @SerializedName("action")
    val action: String,
    @SerializedName("messages")
    val messages: List<String>? = null,
    @SerializedName("user_id")
    val userId: String? = null,
    @SerializedName("message_id")
    val messageId: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("session_id")
    val session_id: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 7L
    }
}

data class TransferPlainAckData(
    @SerializedName("action")
    val action: String,
    @SerializedName("messages")
    val messages: List<BlazeAckMessage>
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 8L
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
