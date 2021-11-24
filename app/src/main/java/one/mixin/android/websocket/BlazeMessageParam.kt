package one.mixin.android.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.api.request.SignalKeyRequest
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import java.io.Serializable
import java.util.UUID

data class BlazeMessageParam(
    val conversation_id: String? = null,
    val recipient_id: String? = null,
    val message_id: String? = null,
    val category: String? = null,
    val data: String? = null,
    val status: String? = null,
    val recipients: ArrayList<BlazeMessageParamSession>? = null,
    val keys: SignalKeyRequest? = null,
    val messages: List<Any>? = null,
    val quote_message_id: String? = null,
    val session_id: String? = null,
    var representative_id: String? = null,
    var conversation_checksum: String? = null,
    var mentions: List<String>? = null,
    var jsep: String? = null,
    var candidate: String? = null,
    var track_id: String? = null,
    var recipient_ids: List<String>? = null,
    val offset: String? = null,
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
