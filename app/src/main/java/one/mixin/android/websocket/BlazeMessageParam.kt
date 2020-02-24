package one.mixin.android.websocket

import java.io.Serializable
import java.util.UUID
import one.mixin.android.api.request.SignalKeyRequest
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus

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
    var mentions: List<String>? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 6L
    }
}

data class BlazeMessageParamSession(
    val user_id: String,
    val session_id: String? = null
)

fun createAckParam(message_id: String, status: String) =
    BlazeMessageParam(message_id = message_id, status = status)

fun createAckListParam(messages: List<BlazeAckMessage>) =
    BlazeMessageParam(messages = messages)

fun createSignalKeyParam(conversationId: String, recipientId: String, cipherText: String) =
    BlazeMessageParam(conversationId, recipientId, UUID.randomUUID().toString(), MessageCategory.SIGNAL_KEY.name,
        cipherText, MessageStatus.SENT.name)

fun createPlainJsonParam(conversationId: String, userId: String, encoded: String, sessionId: String? = null) =
    BlazeMessageParam(conversationId, userId, UUID.randomUUID().toString(), MessageCategory.PLAIN_JSON.name,
        encoded, MessageStatus.SENDING.name, session_id = sessionId)

fun createConsumeSignalKeysParam(recipients: ArrayList<BlazeMessageParamSession>?) =
    BlazeMessageParam(recipients = recipients)

fun createSyncSignalKeysParam(keys: SignalKeyRequest?) =
    BlazeMessageParam(keys = keys)

fun createSignalKeyMessageParam(conversationId: String, messages: ArrayList<BlazeSignalKeyMessage>, conversation_checksum: String) =
    BlazeMessageParam(conversationId, messages = messages, conversation_checksum = conversation_checksum)
