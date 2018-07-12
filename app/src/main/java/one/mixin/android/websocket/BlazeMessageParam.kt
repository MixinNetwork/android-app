package one.mixin.android.websocket

import one.mixin.android.api.request.SignalKeyRequest
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import java.io.Serializable
import java.util.UUID

data class BlazeMessageParam(
    val conversation_id: String?,
    val recipient_id: String?,
    val message_id: String?,
    val category: String?,
    val data: String?,
    val status: String?,
    val recipients: ArrayList<String>? = null,
    val keys: SignalKeyRequest? = null,
    val messages: ArrayList<BlazeSignalKeyMessage>? = null,
    val quote_message_id: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 6L
    }
}

fun createAckParam(message_id: String, status: String) =
    BlazeMessageParam(null, null, message_id, null, null, status)

fun createSignalKeyParam(conversationId: String, recipientId: String, cipherText: String) =
    BlazeMessageParam(conversationId, recipientId, UUID.randomUUID().toString(), MessageCategory.SIGNAL_KEY.name,
        cipherText, MessageStatus.SENT.name)

fun createPlainJsonParam(conversationId: String, userId: String, encoded: String) =
    BlazeMessageParam(conversationId, userId, UUID.randomUUID().toString(), MessageCategory.PLAIN_JSON.name,
        encoded, MessageStatus.SENDING.name)

fun createConsumeSignalKeysParam(recipients: ArrayList<String>?) =
    BlazeMessageParam(null, null, null, null, null, null, recipients)

fun createSyncSignalKeysParam(keys: SignalKeyRequest?) =
    BlazeMessageParam(null, null, null, null, null, null, null, keys)

fun createSignalKeyMessageParam(conversationId: String, messages: ArrayList<BlazeSignalKeyMessage>) =
    BlazeMessageParam(conversationId, null, null, null, null, null, null, null, messages)