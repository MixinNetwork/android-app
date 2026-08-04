package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName
import one.mixin.android.api.request.SignalKeyRequest
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import java.io.Serializable
import java.util.UUID

data class BlazeMessageParam(
    @SerializedName("conversation_id")
    val conversation_id: String? = null,
    @SerializedName("recipient_id")
    val recipient_id: String? = null,
    @SerializedName("message_id")
    val message_id: String? = null,
    @SerializedName("category")
    val category: String? = null,
    @SerializedName("data")
    val data: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("recipients")
    val recipients: ArrayList<BlazeMessageParamSession>? = null,
    @SerializedName("keys")
    val keys: SignalKeyRequest? = null,
    @SerializedName("messages")
    val messages: List<Any>? = null,
    @SerializedName("quote_message_id")
    val quote_message_id: String? = null,
    @SerializedName("session_id")
    val session_id: String? = null,
    @SerializedName("representative_id")
    var representative_id: String? = null,
    @SerializedName("conversation_checksum")
    var conversation_checksum: String? = null,
    @SerializedName("mentions")
    var mentions: List<String>? = null,
    @SerializedName("jsep")
    var jsep: String? = null,
    @SerializedName("candidate")
    var candidate: String? = null,
    @SerializedName("track_id")
    var track_id: String? = null,
    @SerializedName("recipient_ids")
    var recipient_ids: List<String>? = null,
    @SerializedName("offset")
    val offset: String? = null,
    @SerializedName("silent")
    val silent: Boolean? = null,
    @SerializedName("expire_in")
    val expire_in: Long? = null,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 6L
    }
}

data class KrakenParam(
    @SerializedName("jsep")
    var jsep: String? = null,
    @SerializedName("candidate")
    var candidate: String? = null,
    @SerializedName("track_id")
    var track_id: String? = null,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 6L
    }
}

data class BlazeMessageParamSession(
    @SerializedName("user_id")
    val user_id: String,
    @SerializedName("session_id")
    val session_id: String? = null,
)

fun createAckParam(
    message_id: String,
    status: String,
) =
    BlazeMessageParam(message_id = message_id, status = status)

fun createPlainJsonParam(
    conversationId: String,
    userId: String,
    encoded: String,
    sessionId: String? = null,
) =
    BlazeMessageParam(
        conversationId,
        userId,
        UUID.randomUUID().toString(),
        MessageCategory.PLAIN_JSON.name,
        encoded,
        MessageStatus.SENDING.name,
        session_id = sessionId,
    )

fun createConsumeSignalKeysParam(recipients: ArrayList<BlazeMessageParamSession>?) =
    BlazeMessageParam(recipients = recipients)

fun createSyncSignalKeysParam(keys: SignalKeyRequest?) =
    BlazeMessageParam(keys = keys)

fun createSignalKeyMessageParam(
    conversationId: String,
    messages: ArrayList<BlazeSignalKeyMessage>,
    conversation_checksum: String,
) =
    BlazeMessageParam(conversationId, messages = messages, conversation_checksum = conversation_checksum)
