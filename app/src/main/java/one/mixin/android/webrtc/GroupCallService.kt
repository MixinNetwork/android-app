package one.mixin.android.webrtc

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import one.mixin.android.Constants
import one.mixin.android.Constants.ARGS_USER
import one.mixin.android.api.response.UserSession
import one.mixin.android.api.service.ConversationService
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.ParticipantSessionDao
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.nowInUtc
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
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GroupCallService : CallService() {

    private val subscribeExecutors = Executors.newScheduledThreadPool(1)

    @Inject
    lateinit var chatWebSocket: ChatWebSocket
    @Inject
    lateinit var participantSessionDao: ParticipantSessionDao
    @Inject
    lateinit var participantDao: ParticipantDao
    @Inject
    lateinit var conversationApi: ConversationService

    override fun handleIntent(intent: Intent): Boolean {
        val newData = intent.getSerializableExtra(EXTRA_BLAZE) as? BlazeMessageData
        if (newData != null) {
            blazeMessageData = newData
        }

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
        callState.conversationId = blazeMessageData!!.conversationId
        val krakenDataString = String(blazeMessageData!!.data.decodeBase64())
        Timber.d("@@@ krakenDataString: $krakenDataString")
        if (krakenDataString == PUBLISH_PLACEHOLDER) {
            if (!callState.trackId.isNullOrEmpty()) {
                sendSubscribe(callState.trackId!!)
            }
            // TODO
        }
    }

    private fun handlePublish(intent: Intent) {
        if (callState.state == CallState.STATE_DIALING) return

        callState.state = CallState.STATE_DIALING
        val cid = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        require(cid != null)
        callState.conversationId = cid
        callState.isOffer = true
        timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        CallActivity.show(this)
        audioManager.start(true)
        publish()
    }

    private fun publish() {
        getTurnServer { turns ->
            peerConnectionClient.createOffer(
                turns,
                setLocalSuccess = {
                    val blazeMessageParam = BlazeMessageParam(
                        conversation_id = callState.conversationId, category = MessageCategory.KRAKEN_PUBLISH.name,
                        message_id = UUID.randomUUID().toString(), jsep = gson.toJson(Sdp(it.description, it.type.canonicalForm())).base64Encode()
                    )
                    val bm = createKrakenMessage(blazeMessageParam)
                    val data = webSocketChannel(bm) ?: return@createOffer
                    val krakenData = gson.fromJson(String(data.data.decodeBase64()), KrakenData::class.java)
                    subscribe(krakenData)
                }
            )
        }
    }

    private fun subscribe(data: KrakenData) {
        if (data.getSessionDescription().type == SessionDescription.Type.ANSWER) {
            peerConnectionClient.setAnswerSdp(data.getSessionDescription())
            callState.trackId = data.trackId
            subscribeExecutors.scheduleAtFixedRate(SubscribeRunnable(data.trackId), 0, 3, TimeUnit.SECONDS)
        }
    }

    private fun sendSubscribe(trackId: String) {
        val blazeMessageParam = BlazeMessageParam(
            conversation_id = callState.conversationId, category = MessageCategory.KRAKEN_SUBSCRIBE.name,
            message_id = UUID.randomUUID().toString(), track_id = trackId
        )
        val bm = createKrakenMessage(blazeMessageParam)
        val bmData = webSocketChannel(bm) ?: return
        val krakenData = gson.fromJson(String(bmData.data.decodeBase64()), KrakenData::class.java)
        answer(krakenData)
    }

    private fun answer(krakenData: KrakenData) {
        if (krakenData.getSessionDescription().type == SessionDescription.Type.OFFER) {
            peerConnectionClient.createAnswer(
                krakenData.getSessionDescription(),
                setLocalSuccess = {
                    val blazeMessageParam = BlazeMessageParam(
                        conversation_id = callState.conversationId, category = MessageCategory.KRAKEN_ANSWER.name,
                        message_id = UUID.randomUUID().toString(), jsep = gson.toJson(Sdp(it.description, it.type.canonicalForm())).base64Encode(), track_id = krakenData.trackId
                    )
                    val bm = createKrakenMessage(blazeMessageParam)
                    val data = webSocketChannel(bm) ?: return@createAnswer
                }
            )
        }
    }

    private fun handleReceiveInvite(intent: Intent) {
        Timber.d("@@@ handleReceiveInvite")
        if (callState.state == CallState.STATE_RINGING) return

        callState.state = CallState.STATE_RINGING
        val user = intent.getParcelableExtra<User>(ARGS_USER)
        val users = intent.getParcelableArrayListExtra<User>(EXTRA_USERS)
        callState.user = user
        callState.users = users
        callState.conversationId = blazeMessageData?.conversationId
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
        if (callState.isIdle()) return

        val blazeMessageParam = BlazeMessageParam(
            conversation_id = callState.conversationId,
            category = MessageCategory.KRAKEN_END.name,
            message_id = UUID.randomUUID().toString(),
            track_id = callState.trackId
        )
        val bm = createKrakenMessage(blazeMessageParam)
        val bmData = webSocketChannel(bm) ?: return
        val krakenData = gson.fromJson(String(bmData.data.decodeBase64()), KrakenData::class.java)
        callState.state = CallState.STATE_IDLE
        disconnect()
    }

    private fun handleKrakenCancel() {
        if (callState.isIdle()) return

        audioManager.stop()
        val blazeMessageParam = BlazeMessageParam(
            conversation_id = callState.conversationId,
            category = MessageCategory.KRAKEN_CANCEL.name,
            message_id = UUID.randomUUID().toString(),
            track_id = callState.trackId
        )
        val bm = createKrakenMessage(blazeMessageParam)
        val bmData = webSocketChannel(bm) ?: return
        val krakenData = gson.fromJson(String(bmData.data.decodeBase64()), KrakenData::class.java)
        callState.state = CallState.STATE_IDLE
        disconnect()
    }

    private fun handleKrakenDecline() {
        if (callState.isIdle()) return

        audioManager.stop()
        val blazeMessageParam = BlazeMessageParam(
            conversation_id = callState.conversationId,
            category = MessageCategory.KRAKEN_DECLINE.name,
            message_id = UUID.randomUUID().toString(),
            recipient_id = callState.users!![0].userId,
            track_id = callState.trackId
        )
        val bm = createKrakenMessage(blazeMessageParam)
        val bmData = webSocketChannel(bm) ?: return
        val krakenData = gson.fromJson(String(bmData.data.decodeBase64()), KrakenData::class.java)
        callState.state = CallState.STATE_IDLE
        disconnect()
    }

    override fun handleCallLocalFailed() {
        if (callState.isIdle()) return

        audioManager.stop()
        callState.state = CallState.STATE_IDLE
        disconnect()
    }

    override fun handleCallCancel(intent: Intent?) {
        if (callState.isIdle()) return

        audioManager.stop()
        if (callState.trackId != null) {
            sendGroupCallMessage(MessageCategory.KRAKEN_CANCEL.name, trackId = callState.trackId)
        }
        callState.state = CallState.STATE_IDLE
        disconnect()
    }

    override fun handleCallLocalEnd(intent: Intent?) {
        if (callState.isIdle()) return

        if (callState.trackId != null) {
            sendGroupCallMessage(MessageCategory.KRAKEN_END.name, trackId = callState.trackId)
        }
        callState.state = CallState.STATE_IDLE
        disconnect()
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
        stopService<GroupCallService>(this)
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

    inner class SubscribeRunnable(private val trackId: String) : Runnable {
        override fun run() {
            sendSubscribe(trackId)
        }
    }

    private tailrec fun webSocketChannel(blazeMessage: BlazeMessage): BlazeMessageData? {
        blazeMessage.params?.conversation_id?.let {
            blazeMessage.params.conversation_checksum = getCheckSum(it)
        }
        val bm = chatWebSocket.sendMessage(blazeMessage)
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
        return gson.fromJson(bm.data, BlazeMessageData::class.java)
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
}

private const val ACTION_KRAKEN_PUBLISH = "kraken_publish"
private const val ACTION_KRAKEN_RECEIVE_PUBLISH = "kraken_receive_publish"
private const val ACTION_KRAKEN_RECEIVE_INVITE = "kraken_receive_invite"
private const val ACTION_KRAKEN_ACCEPT_INVITE = "kraken_accept_invite"
private const val ACTION_KRAKEN_END = "kraken_end"
private const val ACTION_KRAKEN_CANCEL = "kraken_cancel"
private const val ACTION_KRAKEN_DECLINE = "kraken_decline"

const val PUBLISH_PLACEHOLDER = "PLACEHOLDER"

fun publish(ctx: Context, conversationId: String) =
    startService<GroupCallService>(ctx, ACTION_KRAKEN_PUBLISH) {
        it.putExtra(EXTRA_CONVERSATION_ID, conversationId)
    }

fun receivePublish(ctx: Context, user: User, data: BlazeMessageData) =
    startService<GroupCallService>(ctx, ACTION_KRAKEN_RECEIVE_PUBLISH) {
        it.putExtra(ARGS_USER, user)
        it.putExtra(EXTRA_BLAZE, data)
    }

fun receiveInvite(ctx: Context, data: BlazeMessageData, users: ArrayList<User>? = null) =
    startService<GroupCallService>(ctx, ACTION_KRAKEN_RECEIVE_INVITE) {
        it.putExtra(EXTRA_BLAZE, data)
        it.putExtra(EXTRA_USERS, users)
    }

fun acceptInvite(ctx: Context) = startService<GroupCallService>(ctx, ACTION_KRAKEN_ACCEPT_INVITE) {}

fun krakenEnd(ctx: Context) = startService<GroupCallService>(ctx, ACTION_KRAKEN_END) {}

fun krakenCancel(ctx: Context) = startService<GroupCallService>(ctx, ACTION_KRAKEN_CANCEL) {}

fun krakenDecline(ctx: Context) = startService<GroupCallService>(ctx, ACTION_KRAKEN_DECLINE) {}
