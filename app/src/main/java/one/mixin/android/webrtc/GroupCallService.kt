package one.mixin.android.webrtc

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.JsonElement
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.Disposable
import one.mixin.android.Constants.SLEEP_MILLIS
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.response.UserSession
import one.mixin.android.api.service.ConversationService
import one.mixin.android.crypto.GroupCallSenderKey
import one.mixin.android.crypto.db.RatchetSenderKeyDao
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.ParticipantSessionDao
import one.mixin.android.db.insertAndNotifyConversation
import one.mixin.android.event.CallEvent
import one.mixin.android.event.SenderKeyChange
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.getDeviceId
import one.mixin.android.extension.getSerializableExtraCompat
import one.mixin.android.extension.mainThread
import one.mixin.android.extension.networkConnected
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.call.CallActivity
import one.mixin.android.util.ErrorHandler.Companion.CONVERSATION_CHECKSUM_INVALID_ERROR
import one.mixin.android.util.ErrorHandler.Companion.FORBIDDEN
import one.mixin.android.vo.CallType
import one.mixin.android.vo.ExpiredMessage
import one.mixin.android.vo.KrakenData
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Sdp
import one.mixin.android.vo.createCallMessage
import one.mixin.android.vo.isGroupCallType
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.BlazeMessageData
import one.mixin.android.websocket.BlazeMessageParam
import one.mixin.android.websocket.ChatWebSocket
import one.mixin.android.websocket.LIST_KRAKEN_PEERS
import one.mixin.android.websocket.createKrakenMessage
import one.mixin.android.websocket.createListKrakenPeers
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.whispersystems.libsignal.SignalProtocolAddress
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class GroupCallService : CallService() {
    private val scheduledExecutors = Executors.newScheduledThreadPool(1)
    private val scheduledFutures = mutableMapOf<String, ScheduledFuture<*>>()
    private var reconnectingTimeoutFuture: ScheduledFuture<*>? = null

    @Inject
    lateinit var chatWebSocket: ChatWebSocket

    @Inject
    lateinit var participantSessionDao: ParticipantSessionDao

    @Inject
    lateinit var participantDao: ParticipantDao

    @Inject
    lateinit var ratchetSenderKeyDao: RatchetSenderKeyDao

    @Inject
    lateinit var conversationApi: ConversationService

    private var disposable: Disposable? = null

    private var reconnectTimeoutCount = 0

    private lateinit var callSenderKey: GroupCallSenderKey

    override fun onCreate() {
        super.onCreate()
        callSenderKey =
            GroupCallSenderKey(
                participantSessionDao,
                signalProtocol,
                conversationApi,
                participantDao,
                chatWebSocket,
            ) {
                return@GroupCallSenderKey webSocketChannel(it)
            }
    }

    override fun handleIntent(intent: Intent): Boolean {
        var handled = true
        when (intent.action) {
            ACTION_KRAKEN_PUBLISH -> handlePublish(intent)
            ACTION_KRAKEN_RECEIVE_PUBLISH -> handleReceivePublish(intent)
            ACTION_KRAKEN_RECEIVE_INVITE -> handleReceiveInvite(intent)
            ACTION_KRAKEN_RECEIVE_END -> handleReceiveEnd(intent)
            ACTION_KRAKEN_RECEIVE_CANCEL -> handleReceiveCancel(intent)
            ACTION_KRAKEN_RECEIVE_DECLINE -> handleReceiveDecline(intent)
            ACTION_KRAKEN_ACCEPT_INVITE -> handleAcceptInvite()
            ACTION_KRAKEN_END -> handleLocalEnd()
            ACTION_KRAKEN_CANCEL -> handleKrakenCancel()
            ACTION_KRAKEN_DECLINE -> handleKrakenDecline()
            ACTION_KRAKEN_CANCEL_SILENTLY -> handleCallLocalFailed()
            ACTION_CHECK_PEER -> handleCheckPeer(intent)
            else -> handled = false
        }
        return handled
    }

    private fun handleReceivePublish(intent: Intent) {
        val blazeMessageData = intent.getSerializableExtraCompat(EXTRA_BLAZE, BlazeMessageData::class.java) ?: return
        val cid = blazeMessageData.conversationId
        val userId = blazeMessageData.userId
        callState.addUser(cid, userId)
        startCheckPeers(cid)

        if (callState.isNotIdle() && callState.conversationId != cid) return

        val krakenDataString = String(blazeMessageData.data.decodeBase64())
        if (krakenDataString == PUBLISH_PLACEHOLDER) {
            val trackId = if (callState.isGroupCall()) callState.trackId else null
            if (!trackId.isNullOrEmpty()) {
                sendSubscribe(cid, trackId)
            }
        }
    }

    private fun handlePublish(intent: Intent) {
        Timber.d("$TAG_CALL handlePublish")
        if (callState.state == CallState.STATE_DIALING) return

        if (isDisconnected.compareAndSet(true, false)) {
            callState.state = CallState.STATE_DIALING
            callState.callType = CallType.Group
            updateForegroundNotification()
            val cid = requireNotNull(intent.getStringExtra(EXTRA_CONVERSATION_ID))
            callState.conversationId = cid
            val users = intent.getStringArrayListExtra(EXTRA_USERS)
            users?.let { callState.setPendingUsers(cid, it) }
            callState.isOffer = true
            timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            CallActivity.show(this)
            audioManager.start(true)
            publish(cid)
        }
    }

    private fun publish(conversationId: String) {
        Timber.d("$TAG_CALL publish")
        if (callState.isIdle()) return

        callState.addUser(conversationId, self.userId)
        getTurnServer { turns ->
            createOfferWithTurns(conversationId, turns)
        }
    }

    private fun reconnect(conversationId: String) {
        Timber.d("$TAG_CALL reconnect cid: $conversationId, reconnecting: ${callState.reconnecting}")

        reconnectingTimeoutFuture?.cancel(true)
        reconnectingTimeoutFuture = timeoutExecutor.schedule(ReconnectingTimeoutRunnable(), RECONNECTING_TIMEOUT, TimeUnit.SECONDS)

        callState.reconnecting = true
        callExecutor.execute {
            val pc = peerConnectionClient.getPeerConnection()
            if (pc != null && pc.connectionState() != PeerConnection.PeerConnectionState.CLOSED) {
                Timber.d("$TAG_CALL reconnect pc.close()")
                pc.close()
                Timber.d("$TAG_CALL reconnect pc.close() done")
            }
            publish(conversationId)
        }
    }

    @SuppressLint("AutoDispose")
    private fun createOfferWithTurns(
        conversationId: String,
        turns: List<PeerConnection.IceServer>? = null,
    ) {
        callSenderKey.checkSessionSenderKey(conversationId)
        val key = signalProtocol.getSenderKeyPublic(conversationId, Session.getAccountId()!!)
        peerConnectionClient.createOffer(
            turns,
            setLocalSuccess = {
                val blazeMessageParam =
                    BlazeMessageParam(
                        conversation_id = conversationId,
                        category = MessageCategory.KRAKEN_PUBLISH.name,
                        message_id = UUID.randomUUID().toString(),
                        jsep = gson.toJson(Sdp(it.description, it.type.canonicalForm())).base64Encode(),
                    )
                val bm = createKrakenMessage(blazeMessageParam)
                val data = getBlazeMessageData(bm) ?: return@createOffer
                val krakenData = gson.fromJson(String(data.data.decodeBase64()), KrakenData::class.java)
                subscribe(krakenData, conversationId)
            },
            frameKey = key,
            doWhenSetFailure = {
                reconnect(conversationId)
            },
        )

        disposable =
            RxBus.listen(SenderKeyChange::class.java)
                .subscribe { event ->
                    if (event.conversationId != conversationId) {
                        return@subscribe
                    }
                    if (event.userId != null && event.sessionId != null) {
                        val frameKey = getSenderPublicKey(event.userId, event.sessionId) ?: return@subscribe
                        peerConnectionClient.setReceiverFrameKey(event.userId, event.sessionId, frameKey)
                    } else if (event.userId != null) {
                        callSenderKey.checkSessionSenderKey(event.conversationId)
                    } else {
                        callSenderKey.checkSessionSenderKey(conversationId)
                        val frameKey = signalProtocol.getSenderKeyPublic(conversationId, Session.getAccountId()!!)
                        peerConnectionClient.setSenderFrameKey(frameKey)
                    }
                }
    }

    private fun subscribe(
        data: KrakenData,
        conversationId: String,
    ) {
        if (callState.isIdle()) return

        Timber.d("$TAG_CALL subscribe ${data.getSessionDescription().type == SessionDescription.Type.ANSWER}")
        if (data.getSessionDescription().type == SessionDescription.Type.ANSWER) {
            peerConnectionClient.setAnswerSdp(data.getSessionDescription())
            callState.trackId = data.trackId
            sendSubscribe(conversationId, data.trackId)
            startCheckPeers(conversationId)
        }
    }

    private fun sendSubscribe(
        conversationId: String,
        trackId: String,
    ) {
        Timber.d("$TAG_CALL sendSubscribe")
        if (callState.isIdle()) return

        val blazeMessageParam =
            BlazeMessageParam(
                conversation_id = conversationId,
                category = MessageCategory.KRAKEN_SUBSCRIBE.name,
                message_id = UUID.randomUUID().toString(),
                track_id = trackId,
            )
        val bm = createKrakenMessage(blazeMessageParam)
        Timber.d("$TAG_CALL subscribe track id: $trackId")
        val bmData = getBlazeMessageData(bm) ?: return
        val krakenData = gson.fromJson(String(bmData.data.decodeBase64()), KrakenData::class.java)
        answer(krakenData, conversationId)
    }

    private fun answer(
        krakenData: KrakenData,
        conversationId: String,
    ) {
        if (callState.isIdle()) return

        Timber.d("$TAG_CALL answer ${krakenData.getSessionDescription().type == SessionDescription.Type.OFFER}")
        if (krakenData.getSessionDescription().type == SessionDescription.Type.OFFER) {
            peerConnectionClient.createAnswer(
                null,
                krakenData.getSessionDescription(),
                setLocalSuccess = {
                    val blazeMessageParam =
                        BlazeMessageParam(
                            conversation_id = conversationId,
                            category = MessageCategory.KRAKEN_ANSWER.name,
                            message_id = UUID.randomUUID().toString(),
                            jsep = gson.toJson(Sdp(it.description, it.type.canonicalForm())).base64Encode(),
                            track_id = krakenData.trackId,
                        )
                    val bm = createKrakenMessage(blazeMessageParam)
                    val data = webSocketChannel(bm) ?: return@createAnswer
                    Timber.d("$TAG_CALL answer data: $data")
                },
                doWhenSetFailure = {
                    reconnect(conversationId)
                },
            )
        }
    }

    private fun handleCheckPeer(intent: Intent) {
        val cid = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        requireNotNull(cid)
        getPeers(cid, true)
    }

    private fun startCheckPeers(cid: String) {
        callState.addGroupCallState(cid)
        val existsFuture = scheduledFutures[cid]
        if (existsFuture == null) {
            if (scheduledExecutors.isShutdown) return

            scheduledFutures[cid] =
                scheduledExecutors.scheduleAtFixedRate(
                    ListRunnable(cid),
                    KRAKEN_LIST_INTERVAL,
                    KRAKEN_LIST_INTERVAL,
                    TimeUnit.SECONDS,
                )
        }
    }

    private fun getPeers(
        conversationId: String,
        setWhenCurrentListEmpty: Boolean = false,
    ) {
        val peerList = sendPeer(conversationId) ?: return
        Timber.d("$TAG_CALL getPeers : ${peerList.peers}")
        if (peerList.peers.isNullOrEmpty()) {
            checkSchedules(conversationId)
            return
        }
        val userIdList = arrayListOf<String>()
        peerList.peers.mapTo(userIdList) { it.userId }
        val currentList = callState.getUsers(conversationId)
        if (currentList != null && currentList.size > userIdList.size) {
            if (userIdList.isEmpty()) {
                checkSchedules(conversationId)
            } else {
                callState.setUsersByConversationId(
                    conversationId,
                    userIdList,
                )
            }
        } else if (currentList == null && setWhenCurrentListEmpty) {
            callState.setUsersByConversationId(
                conversationId,
                userIdList,
            )
        }
    }

    private fun sendPeer(conversationId: String): PeerList? {
        val blazeMessageParam =
            BlazeMessageParam(
                conversation_id = conversationId,
                category = MessageCategory.KRAKEN_LIST.name,
                message_id = UUID.randomUUID().toString(),
            )
        val bm = createListKrakenPeers(blazeMessageParam)
        val json = getJsonElement(bm) ?: return null
        return gson.fromJson(json, PeerList::class.java)
    }

    private fun handleReceiveInvite(intent: Intent) {
        Timber.d("$TAG_CALL handleReceiveInvite")
        val cid = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        requireNotNull(cid)
        val userId = intent.getStringExtra(EXTRA_USER_ID)

        if (callState.isBusy()) {
            Timber.d("$TAG_CALL receive a invite from $userId in $cid")
            userId?.let {
                saveMessage(cid, it, MessageCategory.KRAKEN_INVITE.name)

                val blazeMessageParam =
                    BlazeMessageParam(
                        conversation_id = cid,
                        recipient_id = it,
                        category = MessageCategory.KRAKEN_DECLINE.name,
                        message_id = UUID.randomUUID().toString(),
                    )
                val bm = createKrakenMessage(blazeMessageParam)
                val bmData = getBlazeMessageData(bm) ?: return

                @Suppress("UNUSED_VARIABLE")
                val krakenData = gson.fromJson(String(bmData.data.decodeBase64()), KrakenData::class.java)
            }
            return
        }
        val playRing = intent.getBooleanExtra(EXTRA_PLAY_RING, true)
        if (playRing) {
            if (isDisconnected.compareAndSet(true, false)) {
                callState.state = CallState.STATE_RINGING
                callState.callType = CallType.Group
                updateForegroundNotification()
                callState.conversationId = cid
                userId?.let {
                    callState.setInviter(cid, it)
                    callState.addPendingUsers(cid, arrayListOf(it))
                }
                callState.isOffer = false
                timeoutFuture =
                    timeoutExecutor.schedule(
                        TimeoutRunnable(),
                        DEFAULT_TIMEOUT_MINUTES,
                        TimeUnit.MINUTES,
                    )
                CallActivity.show(this, !playRing)
                audioManager.start(false, playRing)
                startCheckPeers(cid)

                userId?.let {
                    saveMessage(cid, it, MessageCategory.KRAKEN_INVITE.name)
                }
            }
        } else {
            callState.state = CallState.STATE_IDLE
            callState.callType = CallType.Group
            callState.conversationId = cid
            userId?.let {
                callState.setInviter(cid, it)
                callState.addPendingUsers(cid, arrayListOf(it))
            }
            callState.isOffer = false
            startCheckPeers(cid)
            CallActivity.show(this, !playRing)
        }
    }

    private fun handleReceiveEnd(intent: Intent) {
        val cid = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        requireNotNull(cid)
        val userId = intent.getStringExtra(EXTRA_USER_ID)
        requireNotNull(userId)

        callState.removeUser(cid, userId)
        checkConversationUserCount(cid)
    }

    private fun handleReceiveCancel(intent: Intent) {
        val cid = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        requireNotNull(cid)
        val userId = intent.getStringExtra(EXTRA_USER_ID)
        requireNotNull(userId)

        callState.removeUser(cid, userId)
        saveMessage(cid, userId, MessageCategory.KRAKEN_CANCEL.name)
        if (callState.isBeforeAnswering()) {
            disconnect()
            checkConversationUserCount(cid)
        }
    }

    private fun handleReceiveDecline(intent: Intent) {
        val cid = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        requireNotNull(cid)
        val userId = intent.getStringExtra(EXTRA_USER_ID)
        requireNotNull(userId)

        callState.removeUser(cid, userId)
        saveMessage(cid, userId, MessageCategory.KRAKEN_DECLINE.name)
    }

    private fun handleAcceptInvite() {
        Timber.d("$TAG_CALL handleAcceptInvite")
        val cid = callState.conversationId
        if (cid == null) {
            Timber.e("$TAG_CALL try accept invite but conversation id is null")
            disconnect()
            return
        }
        if (callState.isAnswering()) return

        isDisconnected.set(false)
        callState.state = CallState.STATE_ANSWERING
        callState.callType = CallType.Group
        updateForegroundNotification()
        audioManager.stop()
        publish(cid)
    }

    override fun handleLocalEnd() {
        Timber.d("$TAG_CALL handleKrakenEnd")
        if (callState.isIdle()) return

        val cid = callState.conversationId
        val trackId = callState.trackId
        if (cid == null || trackId == null) {
            Timber.e("$TAG_CALL try send kraken end message but conversation id is $cid, trackId is $trackId")
            disconnect()
            cid?.let { checkConversationUserCount(it) }
            return
        }

        val duration = System.currentTimeMillis() - (callState.connectedTime ?: 0)
        val blazeMessageParam =
            BlazeMessageParam(
                conversation_id = cid,
                category = MessageCategory.KRAKEN_END.name,
                message_id = UUID.randomUUID().toString(),
                track_id = trackId,
            )

        disconnect()

        saveMessage(cid, self.userId, MessageCategory.KRAKEN_END.name, duration.toString())
        val bm = createKrakenMessage(blazeMessageParam)

        @Suppress("UNUSED_VARIABLE")
        val bmData = getBlazeMessageData(bm)

        checkConversationUserCount(cid)
    }

    private fun handleKrakenCancel() {
        Timber.d("$TAG_CALL handleKrakenCancel")
        if (callState.isIdle()) return

        val cid = callState.conversationId
        if (cid == null) {
            Timber.e("$TAG_CALL try send kraken cancel message but conversation id is null")
            disconnect()
            return
        }

        val blazeMessageParam =
            BlazeMessageParam(
                conversation_id = cid,
                category = MessageCategory.KRAKEN_CANCEL.name,
                message_id = UUID.randomUUID().toString(),
            )

        audioManager.stop()
        disconnect()

        saveMessage(cid, self.userId, MessageCategory.KRAKEN_CANCEL.name)
        val bm = createKrakenMessage(blazeMessageParam)

        @Suppress("UNUSED_VARIABLE")
        val bmData = getBlazeMessageData(bm)

        checkConversationUserCount(cid)
    }

    private fun handleKrakenDecline() {
        Timber.d("$TAG_CALL handleKrakenDecline")
        if (callState.isIdle()) return

        val cid = callState.conversationId
        if (cid == null) {
            Timber.e("$TAG_CALL try send kraken decline message but conversation id is null")
            disconnect()
            return
        }

        val inviter = callState.getInviter(cid)

        audioManager.stop()
        disconnect()

        saveMessage(cid, self.userId, MessageCategory.KRAKEN_DECLINE.name)
        if (inviter != null) {
            val blazeMessageParam =
                BlazeMessageParam(
                    conversation_id = cid,
                    recipient_id = inviter,
                    category = MessageCategory.KRAKEN_DECLINE.name,
                    message_id = UUID.randomUUID().toString(),
                )
            val bm = createKrakenMessage(blazeMessageParam)
            val bmData = getBlazeMessageData(bm) ?: return

            @Suppress("UNUSED_VARIABLE")
            val krakenData = gson.fromJson(String(bmData.data.decodeBase64()), KrakenData::class.java)
        } else {
            Timber.w("$TAG_CALL try send kraken decline message but inviter is null, conversationId: $cid")
        }

        checkConversationUserCount(cid)
    }

    private fun checkConversationUserCount(conversationId: String) {
        val count = callState.getUsersCount(conversationId)
        if (count == 0) {
            checkSchedules(conversationId)
        }
    }

    private fun handleCallLocalFailed() {
        Timber.d("$TAG_CALL handleCallLocalFailed")
        if (callState.isIdle()) return

        val cid = callState.conversationId
        disconnect()
        cid?.let { checkConversationUserCount(cid) }
    }

    override fun needInitWebRtc(action: String) = action != ACTION_CHECK_PEER

    override fun onConnected() {
        val fromReconnecting = callState.reconnecting
        super.onConnected()
        callExecutor.execute {
            reconnectTimeoutCount = 0
            reconnectingTimeoutFuture?.cancel(true)

            if (fromReconnecting) return@execute
            val cid = callState.conversationId ?: return@execute
            val needMute = callState.needMuteWhenJoin(cid)
            if (needMute) {
                muteAudio<GroupCallService>(this, true)
            }
        }
    }

    override fun onClosed() {
        Timber.d("$TAG_CALL onClosed callState.reconnecting: ${callState.reconnecting}")
        if (callState.reconnecting) {
            return
        }

        val conversationId = callState.conversationId
        if (conversationId == null) {
            Timber.d("$TAG_CALL onClosed conversationId is null")
            disconnect()
            return
        }

        callExecutor.execute {
            publish(conversationId)
        }
    }

    override fun onPeerConnectionError(errorMsg: String) {
        super.onPeerConnectionError(errorMsg)
        callExecutor.execute {
            handleLocalEnd()
        }
    }

    override fun onTimeout() {
        callExecutor.execute {
            handleKrakenCancel()
        }
    }

    override fun onTurnServerError() {
        Timber.d("$TAG_CALL onTurnServerError")
        if (callState.reconnecting) {
            SystemClock.sleep(SLEEP_MILLIS)
            val conversationId = callState.conversationId
            if (conversationId == null) {
                Timber.d("$TAG_CALL reconnecting onTurnServerError conversationId is null")
                handleCallLocalFailed()
                return
            }
            publish(conversationId)
        } else {
            handleCallLocalFailed()
        }
    }

    @SuppressLint("AutoDispose")
    override fun onIceFailed() {
        Timber.d(
            "$TAG_CALL onIceFailed callState.isConnected(): ${callState.isConnected()}, disconnected: ${callState.disconnected}, reconnecting: ${callState.reconnecting}",
        )
        if (!callState.isConnected() || callState.reconnecting) return

        val conversationId = callState.conversationId
        if (conversationId == null) {
            Timber.d("$TAG_CALL receive onIceFailed try restart but conversationId is null")
            disconnect()
            return
        }
        val trackId = callState.trackId
        if (trackId == null) {
            Timber.d("$TAG_CALL receive onIceFailed try restart but trackId is null")
            disconnect()
            return
        }

        callExecutor.execute {
            callState.reconnecting = true
            reconnectingTimeoutFuture?.cancel(true)
            reconnectingTimeoutFuture = timeoutExecutor.schedule(ReconnectingTimeoutRunnable(), RECONNECTING_TIMEOUT, TimeUnit.SECONDS)

            peerConnectionClient.createOffer(
                null,
                setLocalSuccess = {
                    val blazeMessageParam =
                        BlazeMessageParam(
                            conversation_id = conversationId,
                            category = MessageCategory.KRAKEN_RESTART.name,
                            message_id = UUID.randomUUID().toString(),
                            track_id = trackId,
                            jsep = gson.toJson(Sdp(it.description, it.type.canonicalForm())).base64Encode(),
                        )
                    val bm = createKrakenMessage(blazeMessageParam)
                    val data = getBlazeMessageData(bm) ?: return@createOffer
                    val krakenData = gson.fromJson(String(data.data.decodeBase64()), KrakenData::class.java)
                    subscribe(krakenData, conversationId)
                },
                doWhenSetFailure = {
                    reconnect(conversationId)
                },
            )
        }
    }

    override fun getSenderPublicKey(
        userId: String,
        sessionId: String,
    ): ByteArray? {
        callState.conversationId?.let {
            callState.addUser(it, userId)
            return signalProtocol.getSenderKeyPublic(it, userId, sessionId)
        }
        return null
    }

    override fun requestResendKey(
        userId: String,
        sessionId: String,
    ) {
        callState.conversationId?.let { cid ->
            val deviceId = sessionId.getDeviceId()
            val address = SignalProtocolAddress(userId, deviceId)
            val status = ratchetSenderKeyDao.getRatchetSenderKey(cid, address.toString())?.status
            if (status == null) {
                one.mixin.android.crypto.requestResendKey(
                    gson,
                    jobManager,
                    ratchetSenderKeyDao,
                    cid,
                    userId,
                    null,
                    sessionId,
                )
            }
        }
    }

    override fun onCallDisconnected() {
        disposable?.dispose()
        reconnectTimeoutCount = 0
    }

    override fun onDestroyed() {
        if (!scheduledExecutors.isShutdown) {
            scheduledExecutors.shutdownNow()
        }
    }

    override fun onIceCandidate(candidate: IceCandidate) {
        callExecutor.execute {
            if (!callState.isGroupCall()) {
                return@execute
            }
            val trackId = callState.trackId ?: return@execute
            val cid = callState.conversationId ?: return@execute

            val blazeMessageParam =
                BlazeMessageParam(
                    conversation_id = cid,
                    category = MessageCategory.KRAKEN_TRICKLE.name,
                    message_id = UUID.randomUUID().toString(),
                    candidate = gson.toJson(candidate).base64Encode(),
                    track_id = trackId,
                )
            val bm = createKrakenMessage(blazeMessageParam)

            @Suppress("UNUSED_VARIABLE")
            val data = webSocketChannel(bm)
        }
    }

    inner class ListRunnable(private val conversationId: String) : Runnable {
        override fun run() {
            getPeers(conversationId)
        }
    }

    private fun getBlazeMessageData(blazeMessage: BlazeMessage): BlazeMessageData? {
        val bm = webSocketChannel(blazeMessage)
        return if (bm != null) {
            gson.fromJson(bm.data, BlazeMessageData::class.java)
        } else {
            null
        }
    }

    private fun getJsonElement(blazeMessage: BlazeMessage): JsonElement? =
        webSocketChannel(blazeMessage)?.data

    private tailrec fun webSocketChannel(blazeMessage: BlazeMessage): BlazeMessage? {
        if (!networkConnected()) {
            Timber.d("$TAG_CALL network not connected, action: ${blazeMessage.action}")
            if (blazeMessage.action == LIST_KRAKEN_PEERS) return null

            callState.reconnecting = false

            SystemClock.sleep(SLEEP_MILLIS)
            return webSocketChannel(blazeMessage)
        }

        blazeMessage.params?.conversation_id?.let {
            blazeMessage.params.conversation_checksum = callSenderKey.getCheckSum(it)
        }
        val bm = chatWebSocket.sendMessage(blazeMessage, if (blazeMessage.action == LIST_KRAKEN_PEERS) 1 else 3)
        Timber.d("$TAG_CALL webSocketChannel $blazeMessage, bm: $bm")
        if (bm == null) {
            Timber.d("$TAG_CALL callExecutor: $callExecutor")
            if (!chatWebSocket.connected && blazeMessage.action == LIST_KRAKEN_PEERS) {
                Timber.d("$TAG_CALL chatWebSocket.connected: ${chatWebSocket.connected}")
                return null
            }
            return if (blazeMessage.action == LIST_KRAKEN_PEERS) {
                null
            } else {
                SystemClock.sleep(SLEEP_MILLIS)
                blazeMessage.id = UUID.randomUUID().toString()
                webSocketChannel(blazeMessage)
            }
        } else if (bm.error != null) {
            Timber.d("$TAG_CALL $bm")
            return when (bm.error.code) {
                CONVERSATION_CHECKSUM_INVALID_ERROR -> {
                    blazeMessage.params?.conversation_id?.let {
                        syncConversation(it)
                        // send sender key
                        callSenderKey.checkSessionSenderKey(it)
                    }
                    blazeMessage.id = UUID.randomUUID().toString()
                    webSocketChannel(blazeMessage)
                }
                FORBIDDEN -> {
                    val cid = blazeMessage.params?.conversation_id
                    Timber.d("$TAG_CALL  ${blazeMessage.action} meet error code FORBIDDEN, conversation id: $cid")
                    cid?.let {
                        RxBus.publish(CallEvent(it, FORBIDDEN, blazeMessage.action))
                    }
                    if (blazeMessage.action == LIST_KRAKEN_PEERS) {
                        callExecutor.execute {
                            handleLocalEnd()
                        }
                    }
                    null
                }
                ERROR_ROOM_FULL -> {
                    val cid = blazeMessage.params?.conversation_id
                    Timber.d("$TAG_CALL try to publish and join a group call, but the room is full, conversation id: $cid.")
                    cid?.let {
                        RxBus.publish(CallEvent(it, ERROR_ROOM_FULL))
                    }
                    disconnect()
                    null
                }
                ERROR_PEER_CLOSED, ERROR_PEER_NOT_FOUND, ERROR_TRACK_NOT_FOUND -> {
                    val cid = blazeMessage.params?.conversation_id
                    if (cid == null) {
                        mainThread {
                            toast(getString(R.string.chat_group_call_remote_peer_closed))
                        }
                        disconnect()
                    } else {
                        if (!callState.reconnecting) {
                            Timber.d("$TAG_CALL reconnecting from bm error: ${bm.error.code}")
                            reconnect(cid)
                        }
                    }
                    null
                }
                else -> {
                    SystemClock.sleep(SLEEP_MILLIS)
                    blazeMessage.id = UUID.randomUUID().toString()
                    webSocketChannel(blazeMessage)
                }
            }
        }
        return bm
    }

    private fun checkSchedules(conversationId: String) {
        Timber.d("$TAG_CALL checkSchedules reconnecting: ${callState.reconnecting}")
        if (callState.inConversationAndAtLeastAnswering(conversationId)) {
            callState.clearUsersKeepSelf(conversationId)
            return
        }

        callState.removeGroupCallState(conversationId)
        val listFuture = scheduledFutures.remove(conversationId)
        listFuture?.cancel(true)

        Timber.d("$TAG_CALL scheduledFutures isEmpty: ${scheduledFutures.isEmpty()}, isIdle: ${callState.isIdle()}")
        if (scheduledFutures.isEmpty() && callState.isIdle()) {
            disconnect()
            stopSelf()
        }
    }

    private fun syncConversation(conversationId: String) {
        try {
            callSenderKey.syncConversation(conversationId)
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    private fun saveMessage(
        cid: String,
        userId: String,
        category: String,
        duration: String? = null,
    ) {
        if (!category.isGroupCallType()) return

        val message =
            createCallMessage(
                UUID.randomUUID().toString(),
                cid,
                userId,
                category,
                "",
                nowInUtc(),
                MessageStatus.READ.name,
                mediaDuration = duration,
            )
        database.conversationDao().findConversationById(cid)?.let {
            val expiredIn = it.expireIn ?: return@let
            if (it.expireIn > 0) {
                database.expiredMessageDao().insert(ExpiredMessage(message.messageId, expiredIn, null))
            }
        }
        database.insertAndNotifyConversation(message)
    }

    private fun checkReconnectingTimeout() {
        Timber.d("$TAG_CALL checkReconnectingTimeout")
        if (callState.isIdle()) return

        val conversationId = callState.conversationId
        if (conversationId == null) {
            Timber.d("$TAG_CALL checkReconnectingTimeout but conversationId is null")
            disconnect()
            return
        }
        callState.reconnecting = true
        callExecutor.execute {
            if (reconnectTimeoutCount == 2) {
                reportReconnectTimeoutInfo()
            }
            reconnectTimeoutCount++

            reconnect(conversationId)
        }
    }

    private fun reportReconnectTimeoutInfo() {
        Timber.d("$TAG_CALL reportReconnectTimeoutInfo")
        peerConnectionClient.getPeerConnection()?.let { pc ->
            val networkState = "{ networkConnected: ${networkConnected()} }"
            val localSdp = "{ localDescription: { description: ${pc.localDescription?.description}, type: ${pc.localDescription?.type} }"
            val remoteSdp = "{ remoteDescription: { description: ${pc.remoteDescription?.description}, type: ${pc.remoteDescription?.type} }"
            val signalingState = "{ signalingState: ${pc.signalingState()} }"
            val iceGatheringState = "{ iceGatheringState: ${pc.iceGatheringState()} }"
            val iceConnectionState = "{ iceConnectionState: ${pc.iceConnectionState()} }"
            val connectionState = "{ connectionState: ${pc.connectionState()} }"
            FirebaseCrashlytics.getInstance().log(
                """
                    WebRTC reconnection timeout info:
                    $networkState,
                    $localSdp,
                    $remoteSdp,
                    $signalingState,
                    $iceGatheringState,
                    $iceConnectionState,
                    $connectionState
                """,
            )
        }
    }

    inner class ReconnectingTimeoutRunnable : Runnable {
        override fun run() {
            checkReconnectingTimeout()
        }
    }

    companion object {
        private const val KRAKEN_LIST_INTERVAL = 30L
        private const val RECONNECTING_TIMEOUT = 60L
    }
}

private const val ACTION_KRAKEN_PUBLISH = "kraken_publish"
private const val ACTION_KRAKEN_RECEIVE_PUBLISH = "kraken_receive_publish"
private const val ACTION_KRAKEN_RECEIVE_INVITE = "kraken_receive_invite"
const val ACTION_KRAKEN_ACCEPT_INVITE = "kraken_accept_invite"
private const val ACTION_KRAKEN_RECEIVE_END = "kraken_receive_end"
private const val ACTION_KRAKEN_RECEIVE_CANCEL = "kraken_receive_cancel"
private const val ACTION_KRAKEN_RECEIVE_DECLINE = "kraken_receive_decline"
private const val ACTION_CHECK_PEER = "check_peer"
const val ACTION_KRAKEN_END = "kraken_end"
const val ACTION_KRAKEN_CANCEL = "kraken_cancel"
const val ACTION_KRAKEN_DECLINE = "kraken_decline"
const val ACTION_KRAKEN_CANCEL_SILENTLY = "kraken_local_cancel"

private const val EXTRA_PLAY_RING = "extra_play_ring"

const val PUBLISH_PLACEHOLDER = "PLACEHOLDER"
const val ERROR_ROOM_FULL = 5002000
const val ERROR_PEER_NOT_FOUND = 5002001
const val ERROR_PEER_CLOSED = 5002002
const val ERROR_TRACK_NOT_FOUND = 5002003

data class PeerList(
    val peers: ArrayList<UserSession>?,
)

fun publish(
    ctx: Context,
    conversationId: String,
    users: ArrayList<String>? = null,
) =
    startService<GroupCallService>(ctx, ACTION_KRAKEN_PUBLISH) {
        it.putExtra(EXTRA_CONVERSATION_ID, conversationId)
        it.putExtra(EXTRA_USERS, users)
    }

fun receivePublish(
    ctx: Context,
    data: BlazeMessageData,
) =
    startService<GroupCallService>(ctx, ACTION_KRAKEN_RECEIVE_PUBLISH) {
        it.putExtra(EXTRA_BLAZE, data)
    }

fun receiveInvite(
    ctx: Context,
    conversationId: String,
    userId: String? = null,
    playRing: Boolean,
) =
    startService<GroupCallService>(ctx, ACTION_KRAKEN_RECEIVE_INVITE) {
        it.putExtra(EXTRA_CONVERSATION_ID, conversationId)
        it.putExtra(EXTRA_USER_ID, userId)
        it.putExtra(EXTRA_PLAY_RING, playRing)
    }

fun receiveEnd(
    ctx: Context,
    conversationId: String,
    userId: String,
) =
    startService<GroupCallService>(ctx, ACTION_KRAKEN_RECEIVE_END) {
        it.putExtra(EXTRA_CONVERSATION_ID, conversationId)
        it.putExtra(EXTRA_USER_ID, userId)
    }

fun receiveCancel(
    ctx: Context,
    conversationId: String,
    userId: String,
) =
    startService<GroupCallService>(ctx, ACTION_KRAKEN_RECEIVE_CANCEL) {
        it.putExtra(EXTRA_CONVERSATION_ID, conversationId)
        it.putExtra(EXTRA_USER_ID, userId)
    }

fun receiveDecline(
    ctx: Context,
    conversationId: String,
    userId: String,
) =
    startService<GroupCallService>(ctx, ACTION_KRAKEN_RECEIVE_DECLINE) {
        it.putExtra(EXTRA_CONVERSATION_ID, conversationId)
        it.putExtra(EXTRA_USER_ID, userId)
    }

fun checkPeers(
    ctx: Context,
    conversationId: String,
) =
    startService<GroupCallService>(ctx, ACTION_CHECK_PEER) {
        it.putExtra(EXTRA_CONVERSATION_ID, conversationId)
    }

fun acceptInvite(ctx: Context) = startService<GroupCallService>(ctx, ACTION_KRAKEN_ACCEPT_INVITE) {}

fun krakenEnd(ctx: Context) = startService<GroupCallService>(ctx, ACTION_KRAKEN_END) {}

fun krakenCancel(ctx: Context) = startService<GroupCallService>(ctx, ACTION_KRAKEN_CANCEL) {}

fun krakenDecline(ctx: Context) = startService<GroupCallService>(ctx, ACTION_KRAKEN_DECLINE) {}

fun krakenCancelSilently(ctx: Context) = startService<GroupCallService>(ctx, ACTION_KRAKEN_CANCEL_SILENTLY) {}
