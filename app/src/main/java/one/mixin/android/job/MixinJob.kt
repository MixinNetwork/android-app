package one.mixin.android.job

import android.util.Log
import com.birbit.android.jobqueue.Params
import com.google.gson.Gson
import com.google.gson.JsonElement
import one.mixin.android.Constants.SLEEP_MILLIS
import one.mixin.android.MixinApplication
import one.mixin.android.api.NetworkException
import one.mixin.android.api.SignalKey
import one.mixin.android.api.WebSocketException
import one.mixin.android.api.createPreKeyBundle
import one.mixin.android.crypto.Base64
import one.mixin.android.crypto.SignalProtocol
import one.mixin.android.extension.fromJson
import one.mixin.android.extension.networkConnected
import one.mixin.android.util.ErrorHandler.Companion.FORBIDDEN
import one.mixin.android.util.Session
import one.mixin.android.vo.LinkState
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.SentSenderKey
import one.mixin.android.vo.SentSenderKeyStatus
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.BlazeMessageParam
import one.mixin.android.websocket.BlazeMessageParamSession
import one.mixin.android.websocket.BlazeSignalKeyMessage
import one.mixin.android.websocket.CREATE_MESSAGE
import one.mixin.android.websocket.PlainDataAction
import one.mixin.android.websocket.TransferPlainData
import one.mixin.android.websocket.createBlazeSignalKeyMessage
import one.mixin.android.websocket.createConsumeSessionSignalKeys
import one.mixin.android.websocket.createConsumeSignalKeysParam
import one.mixin.android.websocket.createSignalKeyMessage
import one.mixin.android.websocket.createSignalKeyMessageParam
import one.mixin.android.websocket.createSignalKeyParam
import timber.log.Timber
import java.util.UUID

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

    protected fun checkSentSenderKey(conversationId: String) {
        val participants = participantDao.getNotSentKeyParticipants(conversationId, Session.getAccountId()!!) ?: return
        if (participants.size == 1) {
            sendSenderKey(conversationId, participants[0].userId)
        } else if (participants.size > 1) {
            sendBatchSenderKey(conversationId, participants)
        }
    }

    protected fun sendGroupSenderKey(conversationId: String) {
        val participants = participantDao.getNotSentKeyParticipants(conversationId, Session.getAccountId()!!) ?: return
        sendBatchSenderKey(conversationId, participants)
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
                        val (cipherText, senderKeyId, _) = signalProtocol.encryptSenderKey(conversationId, key.userId)
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
                SentSenderKey(conversationId, it.recipient_id,
                    SentSenderKeyStatus.SENT.ordinal, it.senderKeyId)
            }
            sentSenderKeyDao.insertList(sentSenderKeys)
        }
    }

    protected fun checkSignalSession(recipientId: String, sessionId: String? = null): Boolean {
        var deviceId = SignalProtocol.DEFAULT_DEVICE_ID
        if (sessionId != null) {
            deviceId = UUID.fromString(sessionId).hashCode()
        }
        if (!signalProtocol.containsSession(recipientId, deviceId)) {
            val blazeMessage = createConsumeSessionSignalKeys(createConsumeSignalKeysParam(arrayListOf(
                BlazeMessageParamSession(recipientId, sessionId)
            )))

            val data = signalKeysChannel(blazeMessage) ?: return false
            val keys = Gson().fromJson<ArrayList<SignalKey>>(data)
            if (keys.isNotEmpty() && keys.count() > 0) {
                val preKeyBundle = createPreKeyBundle(keys[0])
                signalProtocol.processSession(recipientId, preKeyBundle, deviceId)
            } else {
                return false
            }
        }
        return true
    }

    protected fun redirectSendSenderKey(conversationId: String, recipientId: String): Boolean {
        val blazeMessage = createConsumeSessionSignalKeys(createConsumeSignalKeysParam(arrayListOf(BlazeMessageParamSession(recipientId))))
        val data = signalKeysChannel(blazeMessage) ?: return false
        val keys = Gson().fromJson<ArrayList<SignalKey>>(data)
        if (keys.isNotEmpty() && keys.count() > 0) {
            val preKeyBundle = createPreKeyBundle(keys[0])
            signalProtocol.processSession(recipientId, preKeyBundle)
        } else {
            sentSenderKeyDao.insert(SentSenderKey(conversationId, recipientId, SentSenderKeyStatus.UNKNOWN.ordinal))
            Log.e(TAG, "No any signal key from server" + SentSenderKeyStatus.UNKNOWN.ordinal)
            return false
        }

        val (cipherText, senderKeyId, err) = signalProtocol.encryptSenderKey(conversationId, recipientId)
        if (err) return false
        val param = createSignalKeyParam(conversationId, recipientId, cipherText!!)
        val bm = BlazeMessage(UUID.randomUUID().toString(), CREATE_MESSAGE, param)
        val result = deliverNoThrow(bm)
        if (result) {
            sentSenderKeyDao.insert(SentSenderKey(conversationId, recipientId,
                SentSenderKeyStatus.SENT.ordinal, senderKeyId))
        }
        return result
    }

    protected fun sendSenderKey(conversationId: String, recipientId: String): Boolean {
        if (!signalProtocol.containsSession(recipientId)) {
            val blazeMessage = createConsumeSessionSignalKeys(createConsumeSignalKeysParam(arrayListOf(BlazeMessageParamSession(recipientId))))
            val data = signalKeysChannel(blazeMessage) ?: return false
            val keys = Gson().fromJson<ArrayList<SignalKey>>(data)
            if (keys.isNotEmpty() && keys.count() > 0) {
                val preKeyBundle = createPreKeyBundle(keys[0])
                signalProtocol.processSession(recipientId, preKeyBundle)
            } else {
                sentSenderKeyDao.insert(SentSenderKey(conversationId, recipientId, SentSenderKeyStatus.UNKNOWN.ordinal))
                Log.e(TAG, "No any signal key from server" + SentSenderKeyStatus.UNKNOWN.ordinal)
                return false
            }
        }

        val (cipherText, senderKeyId, err) = signalProtocol.encryptSenderKey(conversationId, recipientId)
        if (err) return false
        val param = createSignalKeyParam(conversationId, recipientId, cipherText!!)
        val bm = BlazeMessage(UUID.randomUUID().toString(), CREATE_MESSAGE, param)
        val result = deliverNoThrow(bm)
        if (result) {
            sentSenderKeyDao.insert(SentSenderKey(conversationId, recipientId,
                SentSenderKeyStatus.SENT.ordinal, senderKeyId))
        }
        return result
    }

    protected fun deliverNoThrow(blazeMessage: BlazeMessage): Boolean {
        val bm = chatWebSocket.sendMessage(blazeMessage)
        if (bm == null) {
            if (!MixinApplication.appContext.networkConnected() || !LinkState.isOnline(linkState.state)) {
                throw WebSocketException()
            }
            Thread.sleep(SLEEP_MILLIS)
            return deliverNoThrow(blazeMessage)
        } else if (bm.error != null) {
            return if (bm.error.code == FORBIDDEN) {
                true
            } else {
                Thread.sleep(SLEEP_MILLIS)
                // warning: may caused job leak if server return error data and come to this branch
                deliverNoThrow(blazeMessage)
            }
        }
        return true
    }

    protected fun deliver(blazeMessage: BlazeMessage): Boolean {
        val bm = chatWebSocket.sendMessage(blazeMessage)
        if (bm == null) {
            Thread.sleep(SLEEP_MILLIS)
            throw WebSocketException()
        } else if (bm.error != null) {
            if (bm.error.code == FORBIDDEN) {
                return true
            } else {
                Log.e(TAG, bm.toString())
                Thread.sleep(SLEEP_MILLIS)
                throw NetworkException()
            }
        }
        return true
    }

    private fun signalKeysChannel(blazeMessage: BlazeMessage): JsonElement? {
        val bm = chatWebSocket.sendMessage(blazeMessage)
        if (bm == null) {
            Thread.sleep(SLEEP_MILLIS)
            return signalKeysChannel(blazeMessage)
        } else if (bm.error != null) {
            Log.e(TAG, bm.error.toString())
            return if (bm.error.code == FORBIDDEN) {
                null
            } else {
                Thread.sleep(SLEEP_MILLIS)
                signalKeysChannel(blazeMessage)
            }
        }
        return bm.data
    }

    protected fun makeMessageStatus(status: String, messageId: String) {
        val curStatus = messageDao.findMessageStatusById(messageId)
        if (curStatus != null && curStatus != MessageStatus.READ.name) {
            messageDao.updateMessageStatus(status, messageId)
        }
    }

    protected fun sendNoKeyMessage(conversationId: String, recipientId: String) {
        val plainText = Gson().toJson(TransferPlainData(PlainDataAction.NO_KEY.name))
        val encoded = Base64.encodeBytes(plainText.toByteArray())
        val params = BlazeMessageParam(conversationId, recipientId, UUID.randomUUID().toString(),
            MessageCategory.PLAIN_JSON.name, encoded, MessageStatus.SENDING.name)
        val bm = BlazeMessage(UUID.randomUUID().toString(), CREATE_MESSAGE, params)
        deliverNoThrow(bm)
    }

    internal abstract fun cancel()
}