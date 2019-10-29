package one.mixin.android.job

import android.os.SystemClock
import android.util.Log
import com.birbit.android.jobqueue.Params
import com.google.gson.Gson
import com.google.gson.JsonElement
import java.util.UUID
import one.mixin.android.Constants.SLEEP_MILLIS
import one.mixin.android.MixinApplication
import one.mixin.android.api.NetworkException
import one.mixin.android.api.SignalKey
import one.mixin.android.api.WebSocketException
import one.mixin.android.api.createPreKeyBundle
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.crypto.Base64
import one.mixin.android.extension.fromJson
import one.mixin.android.extension.getDeviceId
import one.mixin.android.extension.networkConnected
import one.mixin.android.util.ErrorHandler.Companion.FORBIDDEN
import one.mixin.android.util.Session
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.LinkState
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.SentSenderKey
import one.mixin.android.vo.SentSenderKeyStatus
import one.mixin.android.vo.createAckJob
import one.mixin.android.vo.isGroup
import one.mixin.android.websocket.BlazeAckMessage
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.BlazeMessageParam
import one.mixin.android.websocket.BlazeMessageParamSession
import one.mixin.android.websocket.BlazeSignalKeyMessage
import one.mixin.android.websocket.CREATE_MESSAGE
import one.mixin.android.websocket.CREATE_SESSION_MESSAGE
import one.mixin.android.websocket.PlainDataAction
import one.mixin.android.websocket.TransferPlainData
import one.mixin.android.websocket.createBlazeSignalKeyMessage
import one.mixin.android.websocket.createConsumeSessionSignalKeys
import one.mixin.android.websocket.createConsumeSignalKeysParam
import one.mixin.android.websocket.createSignalKeyMessage
import one.mixin.android.websocket.createSignalKeyMessageParam
import timber.log.Timber

abstract class MixinJob(params: Params, val jobId: String) : BaseJob(params) {

    protected var isCancel = false

    companion object {
        private const val serialVersionUID = 1L
        val TAG = MixinJob::class.java.simpleName
    }

    override fun onAdded() {
    }

    protected fun removeJob() {
        jobManager.removeJob(jobId)
    }

    override fun shouldRetry(throwable: Throwable): Boolean {
        return if (isCancel) {
            Timber.d("cancel")
            false
        } else {
            Timber.d("no cancel")
            super.shouldRetry(throwable)
        }
    }

    protected fun checkSessionSenderKey(conversationId: String) {
        val participants = participantSessionDao.getNotSendSessionParticipants(conversationId, Session.getSessionId()!!) ?: return
        if (participants.isEmpty()) return
        val requestSignalKeyUsers = arrayListOf<BlazeMessageParamSession>()
        val signalKeyMessages = arrayListOf<BlazeSignalKeyMessage>()
        for (p in participants) {
            if (!signalProtocol.containsSession(p.userId, p.sessionId.getDeviceId())) {
                requestSignalKeyUsers.add(BlazeMessageParamSession(p.userId, p.sessionId))
            } else {
                val (cipherText, senderKeyId, err) = signalProtocol.encryptSenderKey(conversationId, p.userId, p.sessionId.getDeviceId())
                if (err) {
                    requestSignalKeyUsers.add(BlazeMessageParamSession(p.userId, p.sessionId))
                } else {
                    signalKeyMessages.add(createBlazeSignalKeyMessage(p.userId, cipherText!!, senderKeyId, p.sessionId))
                }
            }
        }

        if (requestSignalKeyUsers.isNotEmpty()) {
            val blazeMessage = createConsumeSessionSignalKeys(createConsumeSignalKeysParam(requestSignalKeyUsers))
            val data = signalKeysChannel(blazeMessage)
            if (data != null) {
                val signalKeys = Gson().fromJson<ArrayList<SignalKey>>(data)
                val keys = arrayListOf<BlazeMessageParamSession>()
                if (signalKeys.isNotEmpty()) {
                    for (key in signalKeys) {
                        val preKeyBundle = createPreKeyBundle(key)
                        signalProtocol.processSession(key.userId!!, preKeyBundle)
                        val (cipherText, senderKeyId, _) = signalProtocol.encryptSenderKey(conversationId, key.userId, preKeyBundle.deviceId)
                        signalKeyMessages.add(createBlazeSignalKeyMessage(key.userId, cipherText!!, senderKeyId, key.sessionId))
                        keys.add(BlazeMessageParamSession(key.userId, key.sessionId))
                    }
                } else {
                    Log.e(TAG, "No any group signal key from server")
                }

                val noKeyList = requestSignalKeyUsers.filter { !keys.contains(it) }
                if (noKeyList.isNotEmpty()) {
                    val sentSenderKeys = noKeyList.map {
                        ParticipantSession(conversationId, it.user_id, it.session_id!!, SentSenderKeyStatus.UNKNOWN.ordinal)
                    }
                    participantSessionDao.updateList(sentSenderKeys)
                }
            }
        }
        if (signalKeyMessages.isEmpty()) {
            return
        }
        val bm = createSignalKeyMessage(createSignalKeyMessageParam(conversationId, signalKeyMessages))
        val result = deliverNoThrow(bm)
        if (result) {
            val sentSenderKeys = signalKeyMessages.map {
                ParticipantSession(conversationId, it.recipient_id, it.sessionId!!, SentSenderKeyStatus.SENT.ordinal)
            }
            participantSessionDao.updateList(sentSenderKeys)
        }
    }

    protected fun checkAndSendSenderKey(conversationId: String) {
        checkSessionSenderKey(conversationId)
        val participants = participantDao.getNotSentKeyParticipants(conversationId, Session.getAccountId()!!) ?: return
        if (participants.size == 1) {
            sendSenderKey(conversationId, participants[0].userId)
        } else if (participants.size > 1) {
            sendBatchSenderKey(conversationId, participants)
        }
    }

    protected fun sendSenderKey(conversationId: String, recipientId: String, sessionId: String? = null, isForce: Boolean = false): Boolean {
        if (!signalProtocol.containsSession(recipientId, sessionId.getDeviceId()) || isForce) {
            val blazeMessage = createConsumeSessionSignalKeys(createConsumeSignalKeysParam(arrayListOf(BlazeMessageParamSession(recipientId, sessionId))))
            val data = signalKeysChannel(blazeMessage) ?: return false
            val keys = Gson().fromJson<ArrayList<SignalKey>>(data)
            if (keys.isNotEmpty() && keys.count() > 0) {
                val preKeyBundle = createPreKeyBundle(keys[0])
                signalProtocol.processSession(recipientId, preKeyBundle, sessionId.getDeviceId())
            } else {
                if (!sessionId.isNullOrBlank()) {
                    participantSessionDao.insert(ParticipantSession(conversationId, recipientId, sessionId, SentSenderKeyStatus.UNKNOWN.ordinal))
                }
                sentSenderKeyDao.insert(SentSenderKey(conversationId, recipientId, SentSenderKeyStatus.UNKNOWN.ordinal))
                Log.e(TAG, "No any signal key from server" + SentSenderKeyStatus.UNKNOWN.ordinal)
                return false
            }
        }

        val (cipherText, senderKeyId, err) = signalProtocol.encryptSenderKey(conversationId, recipientId, sessionId.getDeviceId())
        if (err) return false
        val signalKeyMessages = createBlazeSignalKeyMessage(recipientId, cipherText!!, senderKeyId, sessionId)
        val bm = createSignalKeyMessage(createSignalKeyMessageParam(conversationId, arrayListOf(signalKeyMessages)))
        val result = deliverNoThrow(bm)
        if (result) {
            if (!sessionId.isNullOrBlank()) {
                participantSessionDao.insert(ParticipantSession(conversationId, recipientId, sessionId, SentSenderKeyStatus.SENT.ordinal))
            }
            sentSenderKeyDao.insert(SentSenderKey(conversationId, recipientId, SentSenderKeyStatus.SENT.ordinal, senderKeyId))
        }
        return result
    }

    private fun sendBatchSenderKey(conversationId: String, participants: List<Participant>) {
        val requestSignalKeyUsers = arrayListOf<BlazeMessageParamSession>()
        val signalKeyMessages = ArrayList<BlazeSignalKeyMessage>()
        for (p in participants) {
            if (!signalProtocol.containsSession(p.userId)) {
                requestSignalKeyUsers.add(BlazeMessageParamSession(p.userId))
            } else {
                val (cipherText, senderKeyId, err) = signalProtocol.encryptSenderKey(conversationId, p.userId)
                if (err) {
                    requestSignalKeyUsers.add(BlazeMessageParamSession(p.userId))
                } else {
                    signalKeyMessages.add(createBlazeSignalKeyMessage(p.userId, cipherText!!, senderKeyId))
                }
            }
        }

        if (requestSignalKeyUsers.isNotEmpty()) {
            val blazeMessage = createConsumeSessionSignalKeys(createConsumeSignalKeysParam(requestSignalKeyUsers))
            val data = signalKeysChannel(blazeMessage)
            if (data != null) {
                val signalKeys = Gson().fromJson<ArrayList<SignalKey>>(data)
                val keys = arrayListOf<String>()
                if (signalKeys.isNotEmpty()) {
                    for (key in signalKeys) {
                        val preKeyBundle = createPreKeyBundle(key)
                        signalProtocol.processSession(key.userId!!, preKeyBundle)
                        val (cipherText, senderKeyId, _) = signalProtocol.encryptSenderKey(conversationId, key.userId, key.sessionId.getDeviceId())
                        signalKeyMessages.add(createBlazeSignalKeyMessage(key.userId, cipherText!!, senderKeyId))
                        keys.add(key.userId)
                    }
                } else {
                    Log.e(TAG, "No any group signal key from server")
                }

                val noKeyList = requestSignalKeyUsers.filter { !keys.contains(it.user_id) }
                if (noKeyList.isNotEmpty()) {
                    val sentSenderKeys = noKeyList.map {
                        SentSenderKey(conversationId, it.user_id, SentSenderKeyStatus.UNKNOWN.ordinal)
                    }
                    sentSenderKeyDao.insertList(sentSenderKeys)
                }
            }
        }
        if (signalKeyMessages.isEmpty()) {
            return
        }
        val bm = createSignalKeyMessage(createSignalKeyMessageParam(conversationId, signalKeyMessages))
        val result = deliverNoThrow(bm)
        if (result) {
            val sentSenderKeys = signalKeyMessages.map {
                SentSenderKey(
                    conversationId, it.recipient_id,
                    SentSenderKeyStatus.SENT.ordinal, it.senderKeyId
                )
            }
            sentSenderKeyDao.insertList(sentSenderKeys)
        }
    }

    protected fun checkSignalSession(recipientId: String, sessionId: String? = null): Boolean {
        if (!signalProtocol.containsSession(recipientId, sessionId.getDeviceId())) {
            val blazeMessage = createConsumeSessionSignalKeys(
                createConsumeSignalKeysParam(arrayListOf(BlazeMessageParamSession(recipientId, sessionId)))
            )

            val data = signalKeysChannel(blazeMessage) ?: return false
            val keys = Gson().fromJson<ArrayList<SignalKey>>(data)
            if (keys.isNotEmpty() && keys.count() > 0) {
                val preKeyBundle = createPreKeyBundle(keys[0])
                signalProtocol.processSession(recipientId, preKeyBundle)
            } else {
                return false
            }
        }
        return true
    }

    protected tailrec fun deliverNoThrow(blazeMessage: BlazeMessage): Boolean {
        val bm = chatWebSocket.sendMessage(blazeMessage)
        if (bm == null) {
            if (!MixinApplication.appContext.networkConnected() || !LinkState.isOnline(linkState.state)) {
                throw WebSocketException()
            }
            SystemClock.sleep(SLEEP_MILLIS)
            return deliverNoThrow(blazeMessage)
        } else if (bm.error != null) {
            return if (bm.error.code == FORBIDDEN) {
                true
            } else {
                SystemClock.sleep(SLEEP_MILLIS)
                // warning: may caused job leak if server return error data and come to this branch
                return deliverNoThrow(blazeMessage)
            }
        } else {
            return true
        }
    }

    protected fun deliver(blazeMessage: BlazeMessage): Boolean {
        val bm = chatWebSocket.sendMessage(blazeMessage)
        if (bm == null) {
            SystemClock.sleep(SLEEP_MILLIS)
            throw WebSocketException()
        } else if (bm.error != null) {
            if (bm.error.code == FORBIDDEN) {
                return true
            } else {
                Log.e(TAG, bm.toString())
                SystemClock.sleep(SLEEP_MILLIS)
                throw NetworkException()
            }
        }
        return true
    }

    private tailrec fun signalKeysChannel(blazeMessage: BlazeMessage): JsonElement? {
        val bm = chatWebSocket.sendMessage(blazeMessage)
        if (bm == null) {
            SystemClock.sleep(SLEEP_MILLIS)
            return signalKeysChannel(blazeMessage)
        } else if (bm.error != null) {
            return if (bm.error.code == FORBIDDEN) {
                null
            } else {
                SystemClock.sleep(SLEEP_MILLIS)
                return signalKeysChannel(blazeMessage)
            }
        }
        return bm.data
    }

    protected fun makeMessageStatus(status: String, messageId: String) {
        val currentStatus = messageDao.findMessageStatusById(messageId)
        if (currentStatus == MessageStatus.SENDING.name) {
            messageDao.updateMessageStatus(status, messageId)
            sendSessionAck(status, messageId)
        } else if (currentStatus == MessageStatus.SENT.name && (status == MessageStatus.DELIVERED.name || status == MessageStatus.READ.name)) {
            messageDao.updateMessageStatus(status, messageId)
            sendSessionAck(status, messageId)
        } else if (currentStatus == MessageStatus.DELIVERED.name && status == MessageStatus.READ.name) {
            messageDao.updateMessageStatus(status, messageId)
            sendSessionAck(status, messageId)
        }
    }

    private fun sendSessionAck(status: String, messageId: String) {
        val extensionSessionId = Session.getExtensionSessionId()
        extensionSessionId?.let {
            jobDao.insert(createAckJob(CREATE_SESSION_MESSAGE, BlazeAckMessage(messageId, status)))
        }
    }

    protected fun sendNoKeyMessage(conversationId: String, recipientId: String) {
        val plainText = Gson().toJson(TransferPlainData(PlainDataAction.NO_KEY.name))
        val encoded = Base64.encodeBytes(plainText.toByteArray())
        val params = BlazeMessageParam(
            conversationId, recipientId, UUID.randomUUID().toString(),
            MessageCategory.PLAIN_JSON.name, encoded, MessageStatus.SENDING.name
        )
        val bm = BlazeMessage(UUID.randomUUID().toString(), CREATE_MESSAGE, params)
        deliverNoThrow(bm)
    }

    protected fun checkConversation(conversationId: String) {
        val conversation = conversationDao.getConversation(conversationId) ?: return
        if (conversation.isGroup()) {
            syncConversation(conversation)
        } else {
            checkConversationExist(conversation)
        }
    }

    protected fun checkConversationExist(conversation: Conversation) {
        if (conversation.status != ConversationStatus.SUCCESS.ordinal) {
            val request = ConversationRequest(conversationId = conversation.conversationId,
                category = conversation.category, participants = arrayListOf(ParticipantRequest(conversation.ownerId!!, ""))
            )
            val response = conversationApi.create(request).execute().body()
            if (response != null && response.isSuccess && response.data != null && !isCancel) {
                conversationDao.updateConversationStatusById(conversation.conversationId, ConversationStatus.SUCCESS.ordinal)

                val sessionParticipants = response.data!!.participantSessions?.let { resp ->
                    resp.map {
                        ParticipantSession(conversation.conversationId, it.userId, it.sessionId)
                    }
                }
                sessionParticipants?.let {
                    participantSessionDao.replaceAll(conversation.conversationId, it)
                }
            } else {
                throw Exception("Create Conversation Exception")
            }
        }
    }

    private fun syncConversation(conversation: Conversation) {
        val response = conversationApi.getConversation(conversation.conversationId).execute().body()
        if (response != null && response.isSuccess) {
            response.data?.let { data ->
                val remote = data.participants.map {
                    Participant(conversation.conversationId, it.userId, it.role, it.createdAt!!)
                }
                participantDao.replaceAll(conversation.conversationId, remote)

                val sessionParticipants = data.participantSessions?.map {
                    ParticipantSession(conversation.conversationId, it.userId, it.sessionId)
                }
                sessionParticipants?.let {
                    participantSessionDao.replaceAll(conversation.conversationId, it)
                }
            }
        }
    }

    internal abstract fun cancel()
}
