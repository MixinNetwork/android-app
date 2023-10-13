package one.mixin.android.crypto

import android.os.SystemClock
import com.google.gson.Gson
import com.google.gson.JsonElement
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.api.SignalKey
import one.mixin.android.api.WebSocketException
import one.mixin.android.api.createPreKeyBundle
import one.mixin.android.api.response.UserSession
import one.mixin.android.api.service.ConversationService
import one.mixin.android.crypto.db.RatchetSenderKeyDao
import one.mixin.android.crypto.vo.RatchetSenderKey
import one.mixin.android.crypto.vo.RatchetStatus
import one.mixin.android.db.MessageHistoryDao
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.ParticipantSessionDao
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.fromJson
import one.mixin.android.extension.getDeviceId
import one.mixin.android.extension.networkConnected
import one.mixin.android.extension.nowInUtc
import one.mixin.android.job.MessageResult
import one.mixin.android.job.MixinJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SendPlaintextJob
import one.mixin.android.session.Session
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.LinkState
import one.mixin.android.vo.MessageHistory
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.ParticipantSessionSent
import one.mixin.android.vo.SenderKeyStatus
import one.mixin.android.vo.generateConversationChecksum
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.BlazeMessageParamSession
import one.mixin.android.websocket.BlazeSignalKeyMessage
import one.mixin.android.websocket.ChatWebSocket
import one.mixin.android.websocket.PlainDataAction
import one.mixin.android.websocket.PlainJsonMessagePayload
import one.mixin.android.websocket.createBlazeSignalKeyMessage
import one.mixin.android.websocket.createConsumeSessionSignalKeys
import one.mixin.android.websocket.createConsumeSignalKeysParam
import one.mixin.android.websocket.createParamBlazeMessage
import one.mixin.android.websocket.createPlainJsonParam
import one.mixin.android.websocket.createSignalKeyMessage
import one.mixin.android.websocket.createSignalKeyMessageParam
import org.whispersystems.libsignal.SignalProtocolAddress
import timber.log.Timber

class JobSenderKey(
    participantSessionDao: ParticipantSessionDao,
    signalProtocol: SignalProtocol,
    conversationApi: ConversationService,
    participantDao: ParticipantDao,
    private val chatWebSocket: ChatWebSocket,
    private val linkState: LinkState,
    private val messageHistoryDao: MessageHistoryDao,
) : SenderKey(participantSessionDao, signalProtocol, conversationApi, participantDao, chatWebSocket) {
    override fun getJsonElement(blazeMessage: BlazeMessage): JsonElement? {
        return signalKeysChannel(blazeMessage)
    }

    tailrec fun signalKeysChannel(blazeMessage: BlazeMessage): JsonElement? {
        val bm = chatWebSocket.sendMessage(blazeMessage)
        if (bm == null) {
            SystemClock.sleep(Constants.SLEEP_MILLIS)
            return signalKeysChannel(blazeMessage)
        } else if (bm.error != null) {
            return if (bm.error.code == ErrorHandler.FORBIDDEN) {
                null
            } else {
                SystemClock.sleep(Constants.SLEEP_MILLIS)
                return signalKeysChannel(blazeMessage)
            }
        }
        return bm.data
    }

    override fun onBmIsNull() {
        if (!MixinApplication.appContext.networkConnected() || !LinkState.isOnline(linkState.state)) {
            throw WebSocketException()
        }
    }

    override fun onCheckSessionSenderKeySuccess(signalKeyMessages: ArrayList<BlazeSignalKeyMessage>) {
        val messageIds = signalKeyMessages.map { MessageHistory(it.message_id) }
        messageHistoryDao.insertList(messageIds)
    }
}

class GroupCallSenderKey(
    participantSessionDao: ParticipantSessionDao,
    signalProtocol: SignalProtocol,
    conversationApi: ConversationService,
    participantDao: ParticipantDao,
    chatWebSocket: ChatWebSocket,
    private val webSocketChannel: (blazeMessage: BlazeMessage) -> BlazeMessage?,
) : SenderKey(participantSessionDao, signalProtocol, conversationApi, participantDao, chatWebSocket) {
    override fun getJsonElement(blazeMessage: BlazeMessage): JsonElement? {
        return webSocketChannel(blazeMessage)?.data
    }
}

open class SenderKey(
    private val participantSessionDao: ParticipantSessionDao,
    private val signalProtocol: SignalProtocol,
    private val conversationApi: ConversationService,
    private val participantDao: ParticipantDao,
    private val chatWebSocket: ChatWebSocket,
) {
    fun checkSessionSenderKey(conversationId: String) {
        val participants = participantSessionDao.getNotSendSessionParticipants(
            conversationId,
            Session.getSessionId()!!,
        )
        if (participants.isEmpty()) return
        val requestSignalKeyUsers = arrayListOf<BlazeMessageParamSession>()
        val signalKeyMessages = arrayListOf<BlazeSignalKeyMessage>()
        for (p in participants) {
            if (!signalProtocol.containsSession(p.userId, p.sessionId.getDeviceId())) {
                requestSignalKeyUsers.add(BlazeMessageParamSession(p.userId, p.sessionId))
            } else {
                val (cipherText, err) = signalProtocol.encryptSenderKey(
                    conversationId,
                    p.userId,
                    p.sessionId.getDeviceId(),
                )
                if (err) {
                    requestSignalKeyUsers.add(BlazeMessageParamSession(p.userId, p.sessionId))
                } else {
                    signalKeyMessages.add(
                        createBlazeSignalKeyMessage(
                            p.userId,
                            cipherText!!,
                            p.sessionId,
                        ),
                    )
                }
            }
        }

        if (requestSignalKeyUsers.isNotEmpty()) {
            val blazeMessage =
                createConsumeSessionSignalKeys(createConsumeSignalKeysParam(requestSignalKeyUsers))
            val data = getJsonElement(blazeMessage)
            if (data != null) {
                val signalKeys = Gson().fromJson<ArrayList<SignalKey>>(data)
                val keys = arrayListOf<BlazeMessageParamSession>()
                if (!signalKeys.isNullOrEmpty()) {
                    for (key in signalKeys) {
                        val preKeyBundle = createPreKeyBundle(key)
                        signalProtocol.processSession(key.userId!!, preKeyBundle)
                        val (cipherText, _) = signalProtocol.encryptSenderKey(
                            conversationId,
                            key.userId,
                            preKeyBundle.deviceId,
                        )
                        signalKeyMessages.add(
                            createBlazeSignalKeyMessage(
                                key.userId,
                                cipherText!!,
                                key.sessionId,
                            ),
                        )
                        keys.add(BlazeMessageParamSession(key.userId, key.sessionId))
                    }
                } else {
                    Timber.tag(MixinJob.TAG).e(
                        "No any group signal key from server: %s",
                        requestSignalKeyUsers.toString(),
                    )
                }

                val noKeyList = requestSignalKeyUsers.filter { !keys.contains(it) }
                if (noKeyList.isNotEmpty()) {
                    val sentSenderKeys = noKeyList.map {
                        ParticipantSessionSent(
                            conversationId,
                            it.user_id,
                            it.session_id!!,
                            SenderKeyStatus.UNKNOWN.ordinal,
                        )
                    }
                    participantSessionDao.updateParticipantSessionSent(sentSenderKeys)
                }
            }
        }
        if (signalKeyMessages.isEmpty()) {
            return
        }
        val checksum = getCheckSum(conversationId)
        val bm = createSignalKeyMessage(
            createSignalKeyMessageParam(
                conversationId,
                signalKeyMessages,
                checksum,
            ),
        )
        val result = deliverNoThrow(bm)
        if (result.retry) {
            return checkSessionSenderKey(conversationId)
        }
        if (result.success) {
            onCheckSessionSenderKeySuccess(signalKeyMessages)
            val sentSenderKeys = signalKeyMessages.map {
                ParticipantSessionSent(
                    conversationId,
                    it.recipient_id,
                    it.sessionId!!,
                    SenderKeyStatus.SENT.ordinal,
                )
            }
            participantSessionDao.updateParticipantSessionSent(sentSenderKeys)
        }
    }

    fun getCheckSum(conversationId: String): String {
        val sessions = participantSessionDao.getParticipantSessionsByConversationId(conversationId)
        return if (sessions.isEmpty()) {
            ""
        } else {
            generateConversationChecksum(sessions)
        }
    }

    tailrec fun deliverNoThrow(blazeMessage: BlazeMessage): MessageResult {
        val bm = chatWebSocket.sendMessage(blazeMessage)
        if (bm == null) {
            onBmIsNull()
            SystemClock.sleep(Constants.SLEEP_MILLIS)
            return deliverNoThrow(blazeMessage)
        } else if (bm.error != null) {
            return when (bm.error.code) {
                ErrorHandler.CONVERSATION_CHECKSUM_INVALID_ERROR -> {
                    blazeMessage.params?.conversation_id?.let {
                        syncConversation(it)
                    }
                    MessageResult(false, retry = true)
                }
                ErrorHandler.FORBIDDEN -> {
                    MessageResult(true, retry = false)
                }
                else -> {
                    SystemClock.sleep(Constants.SLEEP_MILLIS)
                    // warning: may caused job leak if server return error data and come to this branch
                    return deliverNoThrow(blazeMessage)
                }
            }
        } else {
            return MessageResult(true, retry = false)
        }
    }

    open fun syncConversation(conversationId: String) {
        val response = conversationApi.getConversation(conversationId).execute().body()
        if (response != null && response.isSuccess) {
            response.data?.let { data ->
                val remote = data.participants.map {
                    Participant(conversationId, it.userId, it.role, it.createdAt!!)
                }
                participantDao.replaceAll(conversationId, remote)

                data.participantSessions?.let {
                    syncParticipantSession(conversationId, it)
                }
            }
        }
    }

    fun syncParticipantSession(conversationId: String, data: List<UserSession>) {
        participantSessionDao.deleteByStatus(conversationId)
        val remote = data.map {
            ParticipantSession(conversationId, it.userId, it.sessionId, publicKey = it.publicKey)
        }
        if (remote.isEmpty()) {
            participantSessionDao.deleteByConversationId(conversationId)
            return
        }
        val local = participantSessionDao.getParticipantSessionsByConversationId(conversationId)
        if (local.isEmpty()) {
            participantSessionDao.insertList(remote)
            return
        }
        val common = remote.intersect(local.toSet())
        val remove = local.minus(common)
        val add = remote.minus(common)
        if (remove.isNotEmpty()) {
            participantSessionDao.deleteList(remove)
        }
        if (add.isNotEmpty()) {
            participantSessionDao.insertList(add)
        }
    }

    open fun getJsonElement(blazeMessage: BlazeMessage): JsonElement? {
        return null
    }

    open fun onCheckSessionSenderKeySuccess(signalKeyMessages: ArrayList<BlazeSignalKeyMessage>) {}

    open fun onBmIsNull() {}
}

internal fun requestResendKey(
    gson: Gson,
    jobManager: MixinJobManager,
    ratchetSenderKeyDao: RatchetSenderKeyDao,
    conversationId: String,
    recipientId: String,
    messageId: String?,
    sessionId: String?,
) {
    val plainText = gson.toJson(
        PlainJsonMessagePayload(
            action = PlainDataAction.RESEND_KEY.name,
            messageId = messageId,
        ),
    )
    val encoded = plainText.toByteArray().base64Encode()
    val bm = createParamBlazeMessage(createPlainJsonParam(conversationId, recipientId, encoded, sessionId))
    jobManager.addJobInBackground(SendPlaintextJob(bm))

    val address = SignalProtocolAddress(recipientId, sessionId.getDeviceId())
    val ratchet = RatchetSenderKey(conversationId, address.toString(), RatchetStatus.REQUESTING.name, bm.params?.message_id, nowInUtc())
    ratchetSenderKeyDao.insert(ratchet)
}
