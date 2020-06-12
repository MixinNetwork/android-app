package one.mixin.android.webrtc

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.google.gson.JsonElement
import one.mixin.android.Constants
import one.mixin.android.Constants.ARGS_USER
import one.mixin.android.api.response.UserSession
import one.mixin.android.api.service.ConversationService
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.ParticipantSessionDao
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.decodeBase64
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
import one.mixin.android.vo.User
import one.mixin.android.vo.createCallMessage
import one.mixin.android.vo.generateConversationChecksum
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.BlazeMessageData
import one.mixin.android.websocket.BlazeMessageParam
import one.mixin.android.websocket.ChatWebSocket
import one.mixin.android.websocket.KrakenParam
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
    private var subscribeFuture: ScheduledFuture<*>? = null

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
            ACTION_KRAKEN_ACCEPT_INVITE -> handleAcceptInvite()
            ACTION_KRAKEN_END -> handleKrakenEnd()
            ACTION_KRAKEN_CANCEL -> handleKrakenCancel()
            ACTION_KRAKEN_DECLINE -> handleKrakenDecline()
            else -> handled = false
        }
        return handled
    }

    private fun handleReceivePublish(intent: Intent) {
        val blazeMessageData = intent.getSerializableExtra(EXTRA_BLAZE) as? BlazeMessageData
        requireNotNull(blazeMessageData)
        val cid = blazeMessageData.conversationId
        val krakenDataString = String(blazeMessageData.data.decodeBase64())
        Timber.d("@@@ krakenDataString: $krakenDataString")
        if (krakenDataString == PUBLISH_PLACEHOLDER) {
            if (!callState.trackId.isNullOrEmpty()) {
                callState.conversationId = cid
                sendSubscribe(cid, callState.trackId!!)
            } else {
                callState.addPendingGroupCall(cid)
                val existsFuture = scheduledFutures[cid]
                existsFuture?.cancel(true)
                scheduledFutures[cid] = scheduledExecutors.scheduleAtFixedRate(
                    ListRunnable(cid),
                    0, KRAKEN_LIST_INTERVAL, TimeUnit.SECONDS
                )
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
        callState.isOffer = true
        timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        CallActivity.show(this)
        audioManager.start(true)
        publish()
    }

    private fun publish() {
        Timber.d("@@@ publish")
        getTurnServer { turns ->
            peerConnectionClient.createOffer(
                turns,
                setLocalSuccess = {
                    val blazeMessageParam = BlazeMessageParam(
                        conversation_id = callState.conversationId,
                        category = MessageCategory.KRAKEN_PUBLISH.name,
                        message_id = UUID.randomUUID().toString(),
                        jsep = gson.toJson(Sdp(it.description, it.type.canonicalForm())).base64Encode()
                    )
                    val bm = createKrakenMessage(blazeMessageParam)
                    val data = getBlazeMessageData(bm) ?: return@createOffer
                    val krakenData = gson.fromJson(String(data.data.decodeBase64()), KrakenData::class.java)
                    subscribe(krakenData)
                }
            )
        }
    }

    private fun subscribe(data: KrakenData) {
        Timber.d("@@@ subscribe ${data.getSessionDescription().type == SessionDescription.Type.ANSWER}")
        if (data.getSessionDescription().type == SessionDescription.Type.ANSWER) {
            peerConnectionClient.setAnswerSdp(data.getSessionDescription())
            callState.trackId = data.trackId
            sendSubscribe(callState.conversationId!!, data.trackId)
        }
    }

    private fun sendSubscribe(conversationId: String, trackId: String) {
        Timber.d("@@@ sendSubscribe")
        val blazeMessageParam = BlazeMessageParam(
            conversation_id = conversationId, category = MessageCategory.KRAKEN_SUBSCRIBE.name,
            message_id = UUID.randomUUID().toString(), track_id = trackId
        )
        val bm = createKrakenMessage(blazeMessageParam)
        Timber.d("@@@ subscribe track id: $trackId")
        val bmData = getBlazeMessageData(bm) ?: return
        val krakenData = gson.fromJson(String(bmData.data.decodeBase64()), KrakenData::class.java)
        answer(krakenData)
    }

    private fun answer(krakenData: KrakenData) {
        Timber.d("@@@ answer ${krakenData.getSessionDescription().type == SessionDescription.Type.OFFER}")
        if (krakenData.getSessionDescription().type == SessionDescription.Type.OFFER) {
            peerConnectionClient.createAnswer(
                krakenData.getSessionDescription(),
                setLocalSuccess = {
                    val blazeMessageParam = BlazeMessageParam(
                        conversation_id = callState.conversationId,
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
        if (subscribeFuture == null) {
            subscribeFuture = scheduledExecutors.scheduleAtFixedRate(SubscribeRunnable(callState.conversationId!!, krakenData.trackId), 0, 3, TimeUnit.SECONDS)
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
            val removed = callState.removePendingGroupCall(conversationId)
            if (removed) {
                val listFuture = scheduledFutures.remove(conversationId)
                listFuture?.cancel(true)
            }

            Timber.d("@@@ scheduledFutures isEmpty: ${scheduledFutures.isEmpty()}")

            if (scheduledFutures.isEmpty()) {
                stopForeground(true)
                stopSelf()
            }
            return
        }
        val currentCount = callState.getUserCountByConversationId(conversationId)
        if (currentCount < peerList.peers.size) {
            val userIdList = arrayListOf<String>()
            peerList.peers.mapTo(userIdList) { it.userId }
            callState.setUsersByConversationId(conversationId, userIdList)
        }
    }

    private fun handleReceiveInvite(intent: Intent) {
        Timber.d("@@@ handleReceiveInvite")
        if (callState.state == CallState.STATE_RINGING) return

        callState.state = CallState.STATE_RINGING
        supportsOreo {
            updateForegroundNotification()
        }
        val users = intent.getStringArrayListExtra(EXTRA_USERS)
        val cid = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        requireNotNull(cid)
        callState.conversationId = cid
        callState.setUsersByConversationId(cid, users)
        callState.isOffer = false
        timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        CallActivity.show(this)
        audioManager.start(false)
    }

    private fun handleAcceptInvite() {
        Timber.d("@@@ handleAcceptInvite")
        publish()
    }

    private fun handleKrakenEnd() {
        Timber.d("@@@ handleKrakenEnd")
        if (callState.isIdle()) return

        val blazeMessageParam = BlazeMessageParam(
            conversation_id = callState.conversationId,
            category = MessageCategory.KRAKEN_END.name,
            message_id = UUID.randomUUID().toString(),
            track_id = callState.trackId
        )

        disconnect()

        val bm = createKrakenMessage(blazeMessageParam)
        val bmData = getBlazeMessageData(bm) ?: return
        val krakenData = gson.fromJson(String(bmData.data.decodeBase64()), KrakenData::class.java)
    }

    private fun handleKrakenCancel() {
        Timber.d("@@@ handleKrakenCancel")
        if (callState.isIdle()) return

        val blazeMessageParam = BlazeMessageParam(
            conversation_id = callState.conversationId,
            category = MessageCategory.KRAKEN_CANCEL.name,
            message_id = UUID.randomUUID().toString(),
            track_id = callState.trackId
        )

        audioManager.stop()
        disconnect()

        val bm = createKrakenMessage(blazeMessageParam)
        val bmData = getBlazeMessageData(bm) ?: return
        val krakenData = gson.fromJson(String(bmData.data.decodeBase64()), KrakenData::class.java)
    }

    private fun handleKrakenDecline() {
        Timber.d("@@@ handleKrakenDecline")
        if (callState.isIdle()) return

        val blazeMessageParam = BlazeMessageParam(
            conversation_id = callState.conversationId,
            category = MessageCategory.KRAKEN_DECLINE.name,
            message_id = UUID.randomUUID().toString(),
            recipient_id = callState.getUserByConversationId(callState.conversationId!!)!![0],
            track_id = callState.trackId
        )

        audioManager.stop()
        disconnect()

        val bm = createKrakenMessage(blazeMessageParam)
        val bmData = getBlazeMessageData(bm) ?: return
        val krakenData = gson.fromJson(String(bmData.data.decodeBase64()), KrakenData::class.java)
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

    override fun onCallDisconnected() {
        subscribeFuture?.cancel(true)
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
        val message = createCallMessage(
            UUID.randomUUID().toString(), callState.conversationId!!,
            self.userId, category, "", nowInUtc(), MessageStatus.SENDING.name
        )
        jobManager.addJobInBackground(
            SendMessageJob(
                message, recipientId = recipientId,
                krakenParam = KrakenParam(jsep, candidate, trackId)
            )
        )
    }

    inner class SubscribeRunnable(
        private val conversationId: String,
        private val trackId: String
    ) : Runnable {
        override fun run() {
            sendSubscribe(conversationId, trackId)
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
        } else null
    }

    private fun getJsonElement(blazeMessage: BlazeMessage): JsonElement? =
        webSocketChannel(blazeMessage)?.data

    private tailrec fun webSocketChannel(blazeMessage: BlazeMessage): BlazeMessage? {
        blazeMessage.params?.conversation_id?.let {
            blazeMessage.params.conversation_checksum = getCheckSum(it)
        }
        val bm = chatWebSocket.sendMessage(blazeMessage)
        Timber.d("@@@ webSocketChannel $blazeMessage, bm: $bm")
        if (bm == null) {
            SystemClock.sleep(Constants.SLEEP_MILLIS)
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

    companion object {
        private const val KRAKEN_LIST_INTERVAL = 5L
    }
}

private const val ACTION_KRAKEN_PUBLISH = "kraken_publish"
private const val ACTION_KRAKEN_RECEIVE_PUBLISH = "kraken_receive_publish"
private const val ACTION_KRAKEN_RECEIVE_INVITE = "kraken_receive_invite"
private const val ACTION_KRAKEN_ACCEPT_INVITE = "kraken_accept_invite"
private const val ACTION_KRAKEN_RECEIVE_END = "kraken_receive_end"
private const val ACTION_KRAKEN_END = "kraken_end"
private const val ACTION_KRAKEN_CANCEL = "kraken_cancel"
private const val ACTION_KRAKEN_DECLINE = "kraken_decline"

const val PUBLISH_PLACEHOLDER = "PLACEHOLDER"

data class PeerList(
    val peers: ArrayList<UserSession>?
)

fun publish(ctx: Context, conversationId: String) =
    startService<GroupCallService>(ctx, ACTION_KRAKEN_PUBLISH) {
        it.putExtra(EXTRA_CONVERSATION_ID, conversationId)
    }

fun receivePublish(ctx: Context, user: User, data: BlazeMessageData, foreground: Boolean) =
    startService<GroupCallService>(ctx, ACTION_KRAKEN_RECEIVE_PUBLISH, foreground) {
        it.putExtra(ARGS_USER, user)
        it.putExtra(EXTRA_BLAZE, data)
    }

fun receiveInvite(ctx: Context, conversationId: String, users: ArrayList<String>? = null) =
    startService<GroupCallService>(ctx, ACTION_KRAKEN_RECEIVE_INVITE) {
        it.putExtra(EXTRA_CONVERSATION_ID, conversationId)
        it.putExtra(EXTRA_USERS, users)
    }

fun acceptInvite(ctx: Context) = startService<GroupCallService>(ctx, ACTION_KRAKEN_ACCEPT_INVITE) {}

fun krakenEnd(ctx: Context) = startService<GroupCallService>(ctx, ACTION_KRAKEN_END) {}

fun krakenCancel(ctx: Context) = startService<GroupCallService>(ctx, ACTION_KRAKEN_CANCEL) {}

fun krakenDecline(ctx: Context) = startService<GroupCallService>(ctx, ACTION_KRAKEN_DECLINE) {}
