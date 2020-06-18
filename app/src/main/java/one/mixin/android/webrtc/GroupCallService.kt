package one.mixin.android.webrtc

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.google.gson.JsonElement
import one.mixin.android.Constants
import one.mixin.android.Constants.SLEEP_MILLIS
import one.mixin.android.api.response.UserSession
import one.mixin.android.api.service.ConversationService
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.ParticipantSessionDao
import one.mixin.android.db.insertAndNotifyConversation
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.networkConnected
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.supportsOreo
import one.mixin.android.job.SendMessageJob
import one.mixin.android.ui.call.CallActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.KrakenData
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.Sdp
import one.mixin.android.vo.createCallMessage
import one.mixin.android.vo.generateConversationChecksum
import one.mixin.android.vo.isGroupCallType
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.BlazeMessageData
import one.mixin.android.websocket.BlazeMessageParam
import one.mixin.android.websocket.ChatWebSocket
import one.mixin.android.websocket.KrakenParam
import one.mixin.android.websocket.LIST_KRAKEN_PEERS
import one.mixin.android.websocket.createKrakenMessage
import one.mixin.android.websocket.createListKrakenPeers
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GroupCallService : CallService() {

    private val scheduledExecutors = Executors.newScheduledThreadPool(1)
    private val scheduledFutures = mutableMapOf<String, ScheduledFuture<*>>()

    @Inject
    lateinit var chatWebSocket: ChatWebSocket
    @Inject
    lateinit var participantSessionDao: ParticipantSessionDao
    @Inject
    lateinit var participantDao: ParticipantDao
    @Inject
    lateinit var conversationApi: ConversationService

    override fun handleIntent(intent: Intent): Boolean {
        var handled = true
        when (intent.action) {
            ACTION_KRAKEN_PUBLISH -> handlePublish(intent)
            ACTION_KRAKEN_RECEIVE_PUBLISH -> handleReceivePublish(intent)
            ACTION_KRAKEN_RECEIVE_INVITE -> handleReceiveInvite(intent)
            ACTION_KRAKEN_ACCEPT_INVITE -> handleAcceptInvite(intent)
            ACTION_KRAKEN_END -> handleKrakenEnd()
            ACTION_KRAKEN_CANCEL -> handleKrakenCancel()
            ACTION_KRAKEN_DECLINE -> handleKrakenDecline(intent)
            else -> handled = false
        }
        return handled
    }

    private fun handleReceivePublish(intent: Intent) {
        val blazeMessageData = intent.getSerializableExtra(EXTRA_BLAZE) as? BlazeMessageData
        requireNotNull(blazeMessageData)
        val cid = blazeMessageData.conversationId
        startCheckPeers(cid)

        val krakenDataString = String(blazeMessageData.data.decodeBase64())
        Timber.d("@@@ krakenDataString: $krakenDataString, trackId: ${callState.trackId}")
        if (krakenDataString == PUBLISH_PLACEHOLDER) {
            val trackId = callState.trackId
            if (!trackId.isNullOrEmpty()) {
                callState.conversationId = cid
                sendSubscribe(cid, trackId, blazeMessageData.userId)
            }
        } else {
            callState.conversationId = cid
        }
    }

    private fun handlePublish(intent: Intent) {
        Timber.d("@@@ handlePublish")
        if (callState.state == CallState.STATE_DIALING) return

        callState.state = CallState.STATE_DIALING
        supportsOreo {
            updateForegroundNotification()
        }
        val cid = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        requireNotNull(cid)
        callState.conversationId = cid
        val users = intent.getStringArrayListExtra(EXTRA_USERS)
        callState.addPendingGroupCall(cid)
        users?.let { callState.setInitialGuests(cid, it) }
        callState.isOffer = true
        timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        CallActivity.show(this)
        audioManager.start(true)
        publish(cid)
    }

    private fun publish(conversationId: String) {
        Timber.d("@@@ publish")
        getTurnServer { turns ->
            peerConnectionClient.createOffer(
                turns,
                setLocalSuccess = {
                    val blazeMessageParam = BlazeMessageParam(
                        conversation_id = conversationId,
                        category = MessageCategory.KRAKEN_PUBLISH.name,
                        message_id = UUID.randomUUID().toString(),
                        jsep = gson.toJson(Sdp(it.description, it.type.canonicalForm())).base64Encode()
                    )
                    val bm = createKrakenMessage(blazeMessageParam)
                    val data = getBlazeMessageData(bm) ?: return@createOffer
                    val krakenData = gson.fromJson(String(data.data.decodeBase64()), KrakenData::class.java)
                    subscribe(krakenData, conversationId)
                }
            )
        }
    }

    private fun subscribe(data: KrakenData, conversationId: String) {
        Timber.d("@@@ subscribe ${data.getSessionDescription().type == SessionDescription.Type.ANSWER}")
        if (data.getSessionDescription().type == SessionDescription.Type.ANSWER) {
            peerConnectionClient.setAnswerSdp(data.getSessionDescription())
            callState.trackId = data.trackId
            sendSubscribe(conversationId, data.trackId)
            startCheckPeers(conversationId)
        }
    }

    private fun sendSubscribe(conversationId: String, trackId: String, userId: String? = null) {
        Timber.d("@@@ sendSubscribe")
        val blazeMessageParam = BlazeMessageParam(
            conversation_id = conversationId, category = MessageCategory.KRAKEN_SUBSCRIBE.name,
            message_id = UUID.randomUUID().toString(), track_id = trackId
        )
        val bm = createKrakenMessage(blazeMessageParam)
        Timber.d("@@@ subscribe track id: $trackId")
        val bmData = getBlazeMessageData(bm) ?: return

        userId?.let { u ->
            callState.removeInitialGuest(conversationId, u)
        }

        val krakenData = gson.fromJson(String(bmData.data.decodeBase64()), KrakenData::class.java)
        answer(krakenData, conversationId)
    }

    private fun answer(krakenData: KrakenData, conversationId: String) {
        Timber.d("@@@ answer ${krakenData.getSessionDescription().type == SessionDescription.Type.OFFER}")
        if (krakenData.getSessionDescription().type == SessionDescription.Type.OFFER) {
            peerConnectionClient.createAnswer(
                krakenData.getSessionDescription(),
                setLocalSuccess = {
                    val blazeMessageParam = BlazeMessageParam(
                        conversation_id = conversationId,
                        category = MessageCategory.KRAKEN_ANSWER.name,
                        message_id = UUID.randomUUID().toString(),
                        jsep = gson.toJson(Sdp(it.description, it.type.canonicalForm())).base64Encode(),
                        track_id = krakenData.trackId
                    )
                    val bm = createKrakenMessage(blazeMessageParam)
                    val data = webSocketChannel(bm) ?: return@createAnswer
                }
            )
        }
    }

    private fun startCheckPeers(cid: String) {
        callState.addPendingGroupCall(cid)
        val existsFuture = scheduledFutures[cid]
        if (existsFuture == null) {
            scheduledFutures[cid] = scheduledExecutors.scheduleAtFixedRate(
                ListRunnable(cid),
                0, KRAKEN_LIST_INTERVAL, TimeUnit.SECONDS
            )
        }
    }

    private fun getPeers(conversationId: String) {
        val blazeMessageParam = BlazeMessageParam(
            conversation_id = conversationId,
            category = MessageCategory.KRAKEN_LIST.name,
            message_id = UUID.randomUUID().toString()
        )
        val bm = createListKrakenPeers(blazeMessageParam)
        val json = getJsonElement(bm) ?: return
        val peerList = gson.fromJson(json, PeerList::class.java)
        Timber.d("@@@ getPeers : ${peerList.peers}")
        if (peerList.peers.isNullOrEmpty()) {
            callState.removePendingGroupCall(conversationId)
            val listFuture = scheduledFutures.remove(conversationId)
            listFuture?.cancel(true)

            Timber.d("@@@ scheduledFutures isEmpty: ${scheduledFutures.isEmpty()}")

            if (scheduledFutures.isEmpty()) {
                stopForeground(true)
                stopSelf()
            }
            return
        }
        val userIdList = arrayListOf<String>()
        peerList.peers.mapTo(userIdList) { it.userId }
        callState.setUsersByConversationId(conversationId, userIdList)
    }

    private fun handleReceiveInvite(intent: Intent) {
        Timber.d("@@@ handleReceiveInvite")
        if (callState.state == CallState.STATE_RINGING) return

        callState.state = CallState.STATE_RINGING
        supportsOreo {
            updateForegroundNotification()
        }
        val users = intent.getStringArrayListExtra(EXTRA_USERS)
        val userId = intent.getStringExtra(EXTRA_USER_ID)
        val cid = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        requireNotNull(cid)
        callState.conversationId = cid
        userId?.let { callState.setInviter(cid, it) }
        callState.setUsersByConversationId(cid, users)
        callState.isOffer = false
        timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        CallActivity.show(this)
        if (intent.getBooleanExtra(EXTRA_PLAY_RING, true)) {
            audioManager.start(false)
        }

        userId?.let {
            saveMessage(cid, it, MessageCategory.KRAKEN_INVITE.name)
        }
    }

    private fun handleAcceptInvite(intent: Intent) {
        Timber.d("@@@ handleAcceptInvite")
        val cid = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        requireNotNull(cid)

        audioManager.stop()

        publish(cid)
    }

    private fun handleKrakenEnd() {
        Timber.d("@@@ handleKrakenEnd")
        if (callState.isIdle()) return

        val cid = callState.conversationId
        requireNotNull(cid)
        val duration = System.currentTimeMillis() - (callState.connectedTime ?: 0)
        val blazeMessageParam = BlazeMessageParam(
            conversation_id = cid,
            category = MessageCategory.KRAKEN_END.name,
            message_id = UUID.randomUUID().toString(),
            track_id = callState.trackId
        )

        disconnect()

        saveMessage(cid, self.userId, MessageCategory.KRAKEN_END.name, duration.toString())
        val bm = createKrakenMessage(blazeMessageParam)
        val bmData = getBlazeMessageData(bm) ?: return
        val krakenData = gson.fromJson(String(bmData.data.decodeBase64()), KrakenData::class.java)
    }

    private fun handleKrakenCancel() {
        Timber.d("@@@ handleKrakenCancel")
        if (callState.isIdle()) return

        val cid = callState.conversationId
        requireNotNull(cid)
        val blazeMessageParam = BlazeMessageParam(
            conversation_id = cid,
            category = MessageCategory.KRAKEN_CANCEL.name,
            message_id = UUID.randomUUID().toString(),
            track_id = callState.trackId
        )

        audioManager.stop()
        disconnect()

        saveMessage(cid, self.userId, MessageCategory.KRAKEN_CANCEL.name)
        val bm = createKrakenMessage(blazeMessageParam)
        val bmData = getBlazeMessageData(bm) ?: return
        val krakenData = gson.fromJson(String(bmData.data.decodeBase64()), KrakenData::class.java)
    }

    private fun handleKrakenDecline(intent: Intent) {
        Timber.d("@@@ handleKrakenDecline")
        if (callState.isIdle()) return

        val cid = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        requireNotNull(cid)
        val inviter = callState.getInviter(cid)
        val trackId = callState.trackId

        audioManager.stop()
        disconnect()

        saveMessage(cid, self.userId, MessageCategory.KRAKEN_DECLINE.name)
        if (inviter != null) {
            val blazeMessageParam = BlazeMessageParam(
                conversation_id = cid,
                recipient_id = inviter,
                category = MessageCategory.KRAKEN_DECLINE.name,
                message_id = UUID.randomUUID().toString(),
                track_id = trackId
            )
            val bm = createKrakenMessage(blazeMessageParam)
            val bmData = getBlazeMessageData(bm) ?: return
            val krakenData = gson.fromJson(String(bmData.data.decodeBase64()), KrakenData::class.java)
        } else {
            Timber.w("@@@ Try send kraken decline message but inviter is null, conversationId: $cid")
            getPeers(cid)
        }
    }

    override fun handleCallLocalFailed() {
        Timber.d("@@@ handleCallLocalFailed")
        if (callState.isIdle()) return

        audioManager.stop()
        disconnect()
    }

    override fun handleCallCancel(intent: Intent?) {
        Timber.d("@@@ handleCallCancel")
        if (callState.isIdle()) return

        audioManager.stop()
        if (callState.trackId != null) {
            sendGroupCallMessage(MessageCategory.KRAKEN_CANCEL.name, trackId = callState.trackId)
        }
        disconnect()
    }

    override fun handleCallLocalEnd(intent: Intent?) {
        Timber.d("@@@ handleCallLocalEnd")
        if (callState.isIdle()) return

        if (callState.trackId != null) {
            sendGroupCallMessage(MessageCategory.KRAKEN_END.name, trackId = callState.trackId)
        }
        disconnect()
    }

    override fun onDisconnected() {
        Timber.d("@@@ peerConnection onDisconnected")
    }

    override fun onCallDisconnected() {
    }

    override fun onDestroyed() {
        if (!scheduledExecutors.isShutdown) {
            scheduledExecutors.shutdownNow()
        }
    }

    override fun onIceCandidate(candidate: IceCandidate) {
        callExecutor.execute {
            if (callState.trackId != null) {
                sendGroupCallMessage(
                    MessageCategory.KRAKEN_TRICKLE.name,
                    candidate = gson.toJson(candidate), trackId = callState.trackId
                )
            }
        }
    }

    override fun onPeerConnectionClosed() {
    }

    private fun sendGroupCallMessage(
        category: String,
        jsep: String? = null,
        candidate: String? = null,
        trackId: String? = null,
        recipientId: String? = null
    ) {
        val cid = callState.conversationId ?: return

        val message = createCallMessage(
            UUID.randomUUID().toString(), cid,
            self.userId, category, "", nowInUtc(), MessageStatus.SENDING.name
        )
        jobManager.addJobInBackground(
            SendMessageJob(
                message, recipientId = recipientId,
                krakenParam = KrakenParam(jsep, candidate, trackId)
            )
        )
        saveMessage(cid, self.userId, category)
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
        } else null
    }

    private fun getJsonElement(blazeMessage: BlazeMessage): JsonElement? =
        webSocketChannel(blazeMessage)?.data

    private tailrec fun webSocketChannel(blazeMessage: BlazeMessage): BlazeMessage? {
        if (!networkConnected()) {
            Timber.d("@@@ network not connected, action: ${blazeMessage.action}")
            if (blazeMessage.action == LIST_KRAKEN_PEERS) return null

            SystemClock.sleep(SLEEP_MILLIS)
            return blazeMessage
        }

        blazeMessage.params?.conversation_id?.let {
            blazeMessage.params.conversation_checksum = getCheckSum(it)
        }
        val bm = chatWebSocket.sendMessage(blazeMessage)
        Timber.d("@@@ webSocketChannel $blazeMessage, bm: $bm")
        if (bm == null) {
            SystemClock.sleep(SLEEP_MILLIS)
            blazeMessage.id = UUID.randomUUID().toString()
            return webSocketChannel(blazeMessage)
        } else if (bm.error != null) {
            return when (bm.error.code) {
                ErrorHandler.CONVERSATION_CHECKSUM_INVALID_ERROR -> {
                    blazeMessage.params?.conversation_id?.let {
                        syncConversation(it)
                    }
                    blazeMessage.id = UUID.randomUUID().toString()
                    webSocketChannel(blazeMessage)
                }
                ErrorHandler.FORBIDDEN -> {
                    null
                }
                else -> {
                    SystemClock.sleep(Constants.SLEEP_MILLIS)
                    blazeMessage.id = UUID.randomUUID().toString()
                    webSocketChannel(blazeMessage)
                }
            }
        }
        return bm
    }

    private fun getCheckSum(conversationId: String): String {
        val sessions = participantSessionDao.getParticipantSessionsByConversationId(conversationId)
        return if (sessions.isEmpty()) {
            ""
        } else {
            generateConversationChecksum(sessions)
        }
    }

    private fun syncConversation(conversationId: String) {
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

    private fun syncParticipantSession(conversationId: String, data: List<UserSession>) {
        participantSessionDao.deleteByStatus(conversationId)
        val remote = data.map {
            ParticipantSession(conversationId, it.userId, it.sessionId)
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
        val common = remote.intersect(local)
        val remove = local.minus(common)
        val add = remote.minus(common)
        if (remove.isNotEmpty()) {
            participantSessionDao.deleteList(remove)
        }
        if (add.isNotEmpty()) {
            participantSessionDao.insertList(add)
        }
    }

    private fun saveMessage(cid: String, userId: String, category: String, duration: String? = null) {
        if (!category.isGroupCallType()) return

        val message = createCallMessage(
            UUID.randomUUID().toString(), cid, userId, category,
            "", nowInUtc(), MessageStatus.READ.name,
            mediaDuration = duration
        )
        database.insertAndNotifyConversation(message)
    }

    companion object {
        private const val KRAKEN_LIST_INTERVAL = 5L
    }
}

private const val ACTION_KRAKEN_PUBLISH = "kraken_publish"
private const val ACTION_KRAKEN_RECEIVE_PUBLISH = "kraken_receive_publish"
private const val ACTION_KRAKEN_RECEIVE_INVITE = "kraken_receive_invite"
const val ACTION_KRAKEN_ACCEPT_INVITE = "kraken_accept_invite"
private const val ACTION_KRAKEN_RECEIVE_END = "kraken_receive_end"
const val ACTION_KRAKEN_END = "kraken_end"
const val ACTION_KRAKEN_CANCEL = "kraken_cancel"
const val ACTION_KRAKEN_DECLINE = "kraken_decline"

private const val EXTRA_PLAY_RING = "extra_play_ring"

const val PUBLISH_PLACEHOLDER = "PLACEHOLDER"

data class PeerList(
    val peers: ArrayList<UserSession>?
)

fun publish(ctx: Context, conversationId: String, users: ArrayList<String>? = null) =
    startService<GroupCallService>(ctx, ACTION_KRAKEN_PUBLISH) {
        it.putExtra(EXTRA_CONVERSATION_ID, conversationId)
        it.putExtra(EXTRA_USERS, users)
    }

fun receivePublish(ctx: Context, data: BlazeMessageData, foreground: Boolean) =
    startService<GroupCallService>(ctx, ACTION_KRAKEN_RECEIVE_PUBLISH, foreground) {
        it.putExtra(EXTRA_BLAZE, data)
    }

fun receiveInvite(ctx: Context, conversationId: String, userId: String? = null, users: ArrayList<String>? = null, playRing: Boolean) =
    startService<GroupCallService>(ctx, ACTION_KRAKEN_RECEIVE_INVITE) {
        it.putExtra(EXTRA_CONVERSATION_ID, conversationId)
        it.putExtra(EXTRA_USERS, users)
        it.putExtra(EXTRA_USER_ID, userId)
        it.putExtra(EXTRA_PLAY_RING, playRing)
    }

fun acceptInvite(ctx: Context, conversationId: String) = startService<GroupCallService>(ctx, ACTION_KRAKEN_ACCEPT_INVITE) {
    it.putExtra(EXTRA_CONVERSATION_ID, conversationId)
}

fun krakenEnd(ctx: Context) = startService<GroupCallService>(ctx, ACTION_KRAKEN_END) {}

fun krakenCancel(ctx: Context) = startService<GroupCallService>(ctx, ACTION_KRAKEN_CANCEL) {}

fun krakenDecline(ctx: Context, conversationId: String) = startService<GroupCallService>(ctx, ACTION_KRAKEN_DECLINE) {
    it.putExtra(EXTRA_CONVERSATION_ID, conversationId)
}
