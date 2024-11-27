package one.mixin.android.job

import android.os.SystemClock
import com.birbit.android.jobqueue.Params
import com.google.gson.Gson
import one.mixin.android.Constants.SLEEP_MILLIS
import one.mixin.android.api.ChecksumException
import one.mixin.android.api.NetworkException
import one.mixin.android.api.SignalKey
import one.mixin.android.api.WebSocketException
import one.mixin.android.api.createPreKeyBundle
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.fromJson
import one.mixin.android.extension.getDeviceId
import one.mixin.android.util.ErrorHandler.Companion.BAD_DATA
import one.mixin.android.util.ErrorHandler.Companion.CONVERSATION_CHECKSUM_INVALID_ERROR
import one.mixin.android.util.ErrorHandler.Companion.FORBIDDEN
import one.mixin.android.util.reportException
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.ParticipantSessionSent
import one.mixin.android.vo.SenderKeyStatus
import one.mixin.android.vo.isGroupConversation
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.BlazeMessageParam
import one.mixin.android.websocket.BlazeMessageParamSession
import one.mixin.android.websocket.CREATE_MESSAGE
import one.mixin.android.websocket.PlainDataAction
import one.mixin.android.websocket.PlainJsonMessagePayload
import one.mixin.android.websocket.createBlazeSignalKeyMessage
import one.mixin.android.websocket.createConsumeSessionSignalKeys
import one.mixin.android.websocket.createConsumeSignalKeysParam
import one.mixin.android.websocket.createSignalKeyMessage
import one.mixin.android.websocket.createSignalKeyMessageParam
import timber.log.Timber
import java.util.UUID

abstract class MixinJob(
    params: Params,
    val mixinJobId: String,
) : BaseJob(params.addTags(mixinJobId)) {
    companion object {
        private const val serialVersionUID = 1L
        val TAG = MixinJob::class.java.simpleName
    }

    override fun onAdded() {
    }

    protected fun removeJob() {
        try {
            jobManager.removeJobByMixinJobId(mixinJobId)
        } catch (ignored: Exception) {
        }
    }

    override fun shouldRetry(throwable: Throwable): Boolean {
        return if (isCancelled) {
            false
        } else {
            super.shouldRetry(throwable)
        }
    }

    protected fun sendSenderKey(
        conversationId: String,
        recipientId: String,
        sessionId: String,
    ): Boolean {
        val blazeMessage = createConsumeSessionSignalKeys(createConsumeSignalKeysParam(arrayListOf(BlazeMessageParamSession(recipientId, sessionId))))
        val data = jobSenderKey.signalKeysChannel(blazeMessage) ?: return false
        val keys = Gson().fromJson<ArrayList<SignalKey>>(data)
        if (!keys.isNullOrEmpty()) {
            val preKeyBundle = createPreKeyBundle(keys[0])
            signalProtocol.processSession(recipientId, preKeyBundle)
        } else {
            participantSessionDao().insertParticipantSessionSent(ParticipantSessionSent(conversationId, recipientId, sessionId, SenderKeyStatus.UNKNOWN.ordinal))
            return false
        }

        val (cipherText, err) = signalProtocol.encryptSenderKey(conversationId, recipientId, sessionId.getDeviceId())
        if (err) return false
        val signalKeyMessages = createBlazeSignalKeyMessage(recipientId, cipherText!!, sessionId)
        val checksum = jobSenderKey.getCheckSum(conversationId)
        val bm = createSignalKeyMessage(createSignalKeyMessageParam(conversationId, arrayListOf(signalKeyMessages), checksum))
        val result = jobSenderKey.deliverNoThrow(bm)
        if (result.retry) {
            return sendSenderKey(conversationId, recipientId, sessionId)
        }
        if (result.success) {
            participantSessionDao().insertParticipantSessionSent(ParticipantSessionSent(conversationId, recipientId, sessionId, SenderKeyStatus.SENT.ordinal))
        }
        return result.success
    }

    protected fun checkSignalSession(
        recipientId: String,
        sessionId: String? = null,
    ): Boolean {
        if (!signalProtocol.containsSession(recipientId, sessionId.getDeviceId())) {
            val blazeMessage =
                createConsumeSessionSignalKeys(
                    createConsumeSignalKeysParam(arrayListOf(BlazeMessageParamSession(recipientId, sessionId))),
                )

            val data = jobSenderKey.signalKeysChannel(blazeMessage) ?: return false
            val keys = Gson().fromJson<ArrayList<SignalKey>>(data)
            if (!keys.isNullOrEmpty()) {
                val preKeyBundle = createPreKeyBundle(keys[0])
                signalProtocol.processSession(recipientId, preKeyBundle)
            } else {
                return false
            }
        }
        return true
    }

    protected fun deliver(blazeMessage: BlazeMessage): Boolean {
        blazeMessage.params?.conversation_id?.let {
            blazeMessage.params.conversation_checksum = jobSenderKey.getCheckSum(it)
        }
        val bm = chatWebSocket.sendMessage(blazeMessage)
        if (bm == null) {
            SystemClock.sleep(SLEEP_MILLIS)
            throw WebSocketException()
        } else if (bm.error != null) {
            when (bm.error.code) {
                CONVERSATION_CHECKSUM_INVALID_ERROR -> {
                    blazeMessage.params?.conversation_id?.let {
                        jobSenderKey.syncConversation(it)
                    }
                    throw ChecksumException()
                }
                FORBIDDEN -> {
                    return true
                }
                BAD_DATA -> {
                    reportException(IllegalArgumentException("$blazeMessage, $bm"))
                    return true
                }
                else -> {
                    SystemClock.sleep(SLEEP_MILLIS)
                    Timber.e("$blazeMessage, $bm")
                    throw NetworkException()
                }
            }
        }
        return true
    }

    protected fun sendNoKeyMessage(
        conversationId: String,
        recipientId: String,
    ) {
        val plainText = Gson().toJson(PlainJsonMessagePayload(PlainDataAction.NO_KEY.name))
        val encoded = plainText.base64Encode()
        val params =
            BlazeMessageParam(
                conversationId,
                recipientId,
                UUID.randomUUID().toString(),
                MessageCategory.PLAIN_JSON.name,
                encoded,
                MessageStatus.SENDING.name,
            )
        val bm = BlazeMessage(UUID.randomUUID().toString(), CREATE_MESSAGE, params)
        jobSenderKey.deliverNoThrow(bm)
    }

    protected fun checkConversation(conversationId: String) {
        val conversation = conversationDao().findConversationById(conversationId) ?: return
        if (conversation.isGroupConversation()) {
            jobSenderKey.syncConversation(conversation.conversationId)
        } else {
            checkConversationExist(conversation)
        }
    }

    private fun createConversation(conversation: Conversation): Long? {
        val request =
            ConversationRequest(
                conversationId = conversation.conversationId,
                category = conversation.category,
                participants = arrayListOf(ParticipantRequest(conversation.ownerId!!, "")),
            )
        val response = conversationApi.create(request).execute().body()
        if (response != null && response.isSuccess && response.data != null && !isCancelled) {
            conversationDao().updateConversationStatusById(conversation.conversationId, ConversationStatus.SUCCESS.ordinal)
            conversationDao().updateConversationExpireInById(conversation.conversationId, response.data?.expireIn)

            val sessionParticipants =
                response.data!!.participantSessions.let { resp ->
                    resp?.map {
                        ParticipantSession(conversation.conversationId, it.userId, it.sessionId, publicKey = it.publicKey)
                    }
                }
            sessionParticipants?.let {
                participantSessionDao().replaceAll(conversation.conversationId, it)
            }
            return response.data?.expireIn
        } else {
            throw Exception("Create Conversation Exception")
        }
    }

    protected fun checkConversationExist(conversation: Conversation): Long? {
        return if (conversation.status != ConversationStatus.SUCCESS.ordinal) {
            createConversation(conversation)
        } else {
            conversation.expireIn
        }
    }

    internal abstract fun cancel()
}
