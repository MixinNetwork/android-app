package one.mixin.android.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.api.request.SignalKeyRequest
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import java.io.Serializable
import java.util.UUID

@JsonClass(generateAdapter = true)
data class BlazeMessageParam(
    @Json(name = "conversation_id")
    val conversation_id: String? = null,
    @Json(name = "recipient_id")
    val recipient_id: String? = null,
    @Json(name = "message_id")
    val message_id: String? = null,
    @Json(name = "category")
    val category: String? = null,
    @Json(name = "data")
    val data: String? = null,
    @Json(name = "status")
    val status: String? = null,
    @Json(name = "recipients")
    val recipients: ArrayList<BlazeMessageParamSession>? = null,
    @Json(name = "keys")
    val keys: SignalKeyRequest? = null,
    @Json(name = "messages")
    val messages: List<BlazeSignalKeyMessage>? = null,
    @Json(name = "quote_message_id")
    val quote_message_id: String? = null,
    @Json(name = "session_id")
    val session_id: String? = null,
    @Json(name = "representative_id")
    var representative_id: String? = null,
    @Json(name = "conversation_checksum")
    var conversation_checksum: String? = null,
    @Json(name = "mentions")
    var mentions: List<String>? = null,
    @Json(name = "jsep")
    var jsep: String? = null,
    @Json(name = "candidate")
    var candidate: String? = null,
    @Json(name = "track_id")
    var track_id: String? = null,
    @Json(name = "recipient_ids")
    var recipient_ids: List<String>? = null,
    @Json(name = "offset")
    val offset: String? = null,
    @Json(name = "silent")
    val silent: Boolean? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 6L
    }
}

@JsonClass(generateAdapter = true)
data class KrakenParam(
    @Json(name = "jsep")
    var jsep: String? = null,
    @Json(name = "candidate")
    var candidate: String? = null,
    @Json(name = "track_id")
    var track_id: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 6L
    }
}

@JsonClass(generateAdapter = true)
data class BlazeMessageParamSession(
    @Json(name = "user_id")
    val user_id: String,
    @Json(name = "session_id")
    val session_id: String? = null,
)

fun createAckParam(message_id: String, status: String) =
    BlazeMessageParam(message_id = message_id, status = status)

fun createPlainJsonParam(conversationId: String, userId: String, encoded: String, sessionId: String? = null) =
    BlazeMessageParam(
        conversationId,
        userId,
        UUID.randomUUID().toString(),
        MessageCategory.PLAIN_JSON.name,
        encoded,
        MessageStatus.SENDING.name,
        session_id = sessionId
    )

fun createConsumeSignalKeysParam(recipients: ArrayList<BlazeMessageParamSession>?) =
    BlazeMessageParam(recipients = recipients)

fun createSyncSignalKeysParam(keys: SignalKeyRequest?) =
    BlazeMessageParam(keys = keys)

fun createSignalKeyMessageParam(conversationId: String, messages: ArrayList<BlazeSignalKeyMessage>, conversation_checksum: String) =
    BlazeMessageParam(conversationId, messages = messages, conversation_checksum = conversation_checksum)
