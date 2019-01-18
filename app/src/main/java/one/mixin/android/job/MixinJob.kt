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
import one.mixin.android.extension.fromJson
import one.mixin.android.extension.networkConnected
import one.mixin.android.extension.nowInUtc
import one.mixin.android.util.ErrorHandler.Companion.FORBIDDEN
import one.mixin.android.util.Session
import one.mixin.android.vo.LinkState
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.ParticipantItem
import one.mixin.android.vo.SentSenderKeyStatus
import one.mixin.android.vo.SentSessionSenderKey
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.BlazeMessageParam
import one.mixin.android.websocket.BlazeSignalKeyMessage
import one.mixin.android.websocket.CONSUME_SESSION_SIGNAL_KEYS
import one.mixin.android.websocket.CREATE_MESSAGE
import one.mixin.android.websocket.PlainDataAction
import one.mixin.android.websocket.TransferPlainData
import one.mixin.android.websocket.createBlazeSignalKeyMessage
import one.mixin.android.websocket.createConsumeSignalKeys
import one.mixin.android.websocket.createConsumeSignalKeysParam
import one.mixin.android.websocket.createSignalKeyMessage
import one.mixin.android.websocket.createSignalKeyMessageParam
import one.mixin.android.websocket.createSignalKeyParam
import timber.log.Timber
import java.io.IOException
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
        val participantIds = participants.map { it.userId }
        try {
            val call = accountService.getSessions(participantIds).execute()
            val response = call.body()
            if (response != null && response.isSuccess) {
                response.data?.let { list ->
                    sessionDao.insertList(list)
                }
            }
        } catch (e: IOException) {
            return
        }
        if (participants.size > 1) {
            sendBatchSenderKey(conversationId, participants)
        }
    }

    protected fun sendGroupSenderKey(conversationId: String) {
        participantDao.getRealParticipants(conversationId).map { it.userId }.let { ids ->
            try {
                val call = accountService.getSessions(ids).execute()
                val response = call.body()
                if (response != null && response.isSuccess) {
                    response.data?.let { list ->
                        sessionDao.insertList(list)
                    }
                }
            } catch (e: IOException) {
                Timber.e(e)
            }
        }
        val participants = participantDao.getNotSentKeyParticipants(conversationId, Session.getAccountId()!!) ?: return
        sendBatchSenderKey(conversationId, participants)
    }

    private fun sendBatchSenderKey(conversationId: String, participants: List<ParticipantItem>) {
        val requestSignalKeyUsers = arrayListOf<ParticipantItem>()
        val signalKeyMessages = ArrayList<BlazeSignalKeyMessage>()
        for (p in participants) {
            if (!signalProtocol.containsSession(p.userId, p.deviceId)) {
                requestSignalKeyUsers.add(p)
            } else {
                val (cipherText, senderKeyId, err) = signalProtocol.encryptSenderKey(conversationId, p.userId, p.deviceId)
                if (err) {
                    requestSignalKeyUsers.add(p)
                } else {
                    signalKeyMessages.add(createBlazeSignalKeyMessage(p.userId, cipherText!!, null, p.sessionId))
                }
            }
        }
        if (requestSignalKeyUsers.isNotEmpty()) {
            val blazeMessage = createConsumeSignalKeys(createConsumeSignalKeysParam(requestSignalKeyUsers.map {
                one.mixin.android.vo.Session(it.sessionId, it.userId, it.deviceId)
            }))
            val data = signalKeysChannel(blazeMessage)
            if (data != null) {
                val signalKeys = Gson().fromJson<ArrayList<SignalKey>>(data)
                val keys = arrayListOf<String>()
                if (signalKeys.isNotEmpty()) {
                    for (key in signalKeys) {
                        val preKeyBundle = createPreKeyBundle(key)
                        signalProtocol.processSession(key.userId!!, preKeyBundle, key.sessionId.hashCode())
                        val (cipherText, senderKeyId, _) = signalProtocol.encryptSenderKey(conversationId, key.userId, key.sessionId.hashCode())
                        signalKeyMessages.add(createBlazeSignalKeyMessage(key.userId, cipherText!!, senderKeyId, key.sessionId))
                        keys.add(key.userId)
                    }
                } else {
                    Log.e(TAG, "No any group signal key from server")
                }

                val noKeyList = requestSignalKeyUsers.filter { !keys.contains(it.userId) }
                if (noKeyList.isNotEmpty()) {
                    // Maybe todo
                }
            }
        }
        if (signalKeyMessages.isEmpty()) {
            return
        }
        val bm = createSignalKeyMessage(createSignalKeyMessageParam(conversationId, signalKeyMessages))
        Timber.d(Gson().toJson(bm))
        val result = deliverNoThrow(bm)
        if (result) {
            val sentSenderKeys = signalKeyMessages.map {
                SentSessionSenderKey(conversationId, it.recipient_id, it.sessionId!!, "1", null)
            }
            sentSessionSenderKeyDao.insertList(sentSenderKeys)
        }
    }

    protected fun checkSignalSession(recipientId: String): Boolean {
        if (!signalProtocol.containsSession(recipientId)) {
            val blazeMessage = createConsumeSignalKeys(createConsumeSignalKeysParam(sessionDao.findSessionByUserId(recipientId)!!))
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

    protected fun redirectSendSenderKey(conversationId: String, recipientId: String): Boolean {
        var sessions: List<one.mixin.android.vo.Session>? = null
        try {
            val call = accountService.getSessions(listOf(recipientId)).execute()
            val response = call.body()
            if (response != null && response.isSuccess) {
                response.data?.let { list ->
                    sessions = list
                    sessionDao.insertList(list)
                }
            }
        } catch (e: IOException) {
            Timber.e(e)
        }
        if (sessions?.isNotEmpty() == true) {
            val blazeMessage = createConsumeSignalKeys(createConsumeSignalKeysParam(sessions))
            val data = signalKeysChannel(blazeMessage) ?: return false
            val keys = Gson().fromJson<ArrayList<SignalKey>>(data)
            if (keys.isNotEmpty() && keys.count() > 0) {
                val preKeyBundle = createPreKeyBundle(keys[0])
                signalProtocol.processSession(recipientId, preKeyBundle)
            } else {
                // Todo
                // sentSenderKeyDao.insert(SentSenderKey(conversationId, recipientId, SentSenderKeyStatus.UNKNOWN.ordinal))
                Log.e(TAG, "No any signal key from server" + SentSenderKeyStatus.UNKNOWN.ordinal)
                return false
            }

            for (session in sessions!!) {
                val (cipherText, senderKeyId, err) = signalProtocol.encryptSenderKey(conversationId, session.userId, session.deviceId)
                if (err) return false
                val param = createSignalKeyParam(conversationId, session.userId, cipherText!!, session.sessionId)
                val bm = BlazeMessage(UUID.randomUUID().toString(), CREATE_MESSAGE, param)
                val result = deliverNoThrow(bm)
                if (result) {
                    // Todo
                    sentSessionSenderKeyDao.insert(SentSessionSenderKey(conversationId, session.userId, session.sessionId, "1", null))
                }
            }
            return true
        }
        return false
    }

    protected fun sendSenderKey(conversationId: String, recipientId: String): Boolean {
        val sessions = syncUserSession(recipientId)?.toMutableList()
        syncUserSession(Session.getAccountId()!!) // Todo
        sessionDao.findSecondarySessionByUserId(Session.getAccountId()!!)?.let { senderPrimarySession ->
            sessions?.addAll(0, senderPrimarySession)
        }
        Timber.d(Gson().toJson(sessions))
        if (!sessions.isNullOrEmpty()) {
            for (session in sessions) {
                if (!signalProtocol.containsSession(session.userId, session.deviceId)) {
                    val param = createSignalKeyParam(ArrayList(listOf(session)))
                    val blazeMessage = BlazeMessage(UUID.randomUUID().toString(), CONSUME_SESSION_SIGNAL_KEYS, param)
                    Timber.d("blaze:${Gson().toJson(blazeMessage)}")
                    val data = signalKeysChannel(blazeMessage) ?: return false
                    val keys = Gson().fromJson<ArrayList<SignalKey>>(data)
                    if (keys.isNotEmpty() && keys.count() > 0) {
                        val preKeyBundle = createPreKeyBundle(keys[0])
                        signalProtocol.processSession(session.userId, preKeyBundle, session.deviceId)
                    } else {
                        sentSessionSenderKeyDao.insert(SentSessionSenderKey(conversationId, session.userId, session.sessionId, "0", null, nowInUtc()))
                        Log.e(TAG, "No any signal key from server" + SentSenderKeyStatus.UNKNOWN.ordinal)
                        return false
                    }
                }
                val (cipherText, senderKeyId, err) = signalProtocol.encryptSenderKey(conversationId, session.userId, session.deviceId)
                if (err) {
                    Timber.e("err")
                    return false
                }
                val param = createSignalKeyParam(conversationId, session.userId, cipherText!!, session.sessionId)
                val bm = BlazeMessage(UUID.randomUUID().toString(), CREATE_MESSAGE, param)
                Timber.d("send:${Gson().toJson(bm)}")
                val result = deliverNoThrow(bm)
                if (result) {
                    sentSessionSenderKeyDao.insert(SentSessionSenderKey(conversationId, session.userId, session.sessionId, "1", null, nowInUtc()))
                }
            }
        }
        return true
    }

    private fun syncUserSession(userId: String): List<one.mixin.android.vo.Session>? {
        var sessions = sessionDao.findSessionByUserId(userId)
        if (sessions.isNullOrEmpty()) {
            try {
                val call = accountService.getSessions(listOf(userId)).execute()
                val response = call.body()
                if (response != null && response.isSuccess) {
                    response.data?.let { list ->
                        sessions = list
                        sessionDao.insertList(list)
                    }
                }
            } catch (e: IOException) {
                Timber.e(e)
            }
        }
        return sessions
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