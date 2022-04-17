package one.mixin.android.webrtc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.twilio.audioswitch.AudioSwitch
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.service.AccountService
import one.mixin.android.crypto.SignalProtocol
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.isServiceRunning
import one.mixin.android.extension.notificationManager
import one.mixin.android.extension.supportsOreo
import one.mixin.android.job.BlazeMessageService
import one.mixin.android.job.MixinJobManager
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.session.Session
import one.mixin.android.ui.call.CallNotificationBuilder
import one.mixin.android.util.ChannelManager
import one.mixin.android.util.reportException
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.vo.Message
import one.mixin.android.vo.TurnServer
import one.mixin.android.vo.User
import one.mixin.android.vo.toUser
import one.mixin.android.widget.PipCallView
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.StatsReport
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

abstract class CallService : LifecycleService(), PeerConnectionClient.PeerConnectionEvents {

    protected val callExecutor: ThreadPoolExecutor = Executors.newFixedThreadPool(1) as ThreadPoolExecutor
    protected val timeoutExecutor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private val observeStatsDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    protected var timeoutFuture: ScheduledFuture<*>? = null

    protected lateinit var peerConnectionClient: PeerConnectionClient

    protected lateinit var audioManager: CallAudioManager

    @Inject
    lateinit var audioSwitch: AudioSwitch
    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    lateinit var database: MixinDatabase
    @Inject
    lateinit var accountService: AccountService
    @Inject
    lateinit var callState: CallStateLiveData
    @Inject
    lateinit var callDebugState: CallDebugLiveData
    @Inject
    lateinit var conversationRepo: ConversationRepository
    @Inject
    lateinit var signalProtocol: SignalProtocol

    protected val gson = Gson()

    protected lateinit var self: User

    private var isDestroyed = AtomicBoolean(false)
    protected var isDisconnected = AtomicBoolean(true)

    private val pipCallView by lazy {
        PipCallView.get()
    }

    override fun onCreate() {
        super.onCreate()
        Session.getAccount()?.toUser().let { user ->
            if (user == null) {
                stopSelf()
            } else {
                self = user
            }
        }

        callDebugState.observe(this) { type ->
            if (::peerConnectionClient.isInitialized) {
                peerConnectionClient.callDebugState = type
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val action = intent?.action
        if (intent == null || action == null) {
            return START_NOT_STICKY
        }
        if (isDestroyed.get()) {
            stopSelf()
            return Service.START_NOT_STICKY
        }

        if (needInitWebRtc(action)) {
            initWebRtc()
        }

        callExecutor.execute {
            if (!handleIntent(intent)) {
                when (intent.action) {
                    ACTION_CALL_DISCONNECT -> disconnect()
                    ACTION_MUTE_AUDIO -> handleMuteAudio(intent)
                    ACTION_SPEAKERPHONE -> handleSpeakerphone(intent)
                    ACTION_LOG_CALL_STATE -> handleLogCallState()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Timber.d("$TAG_CALL onDestroy")
        super.onDestroy()
        if (isDestroyed.compareAndSet(false, true)) {
            Timber.d("$TAG_CALL real onDestroy")
            if (::audioManager.isInitialized) {
                audioManager.release()
            }
            if (::peerConnectionClient.isInitialized) {
                peerConnectionClient.release()
            }

            onDestroyed()
        }
    }

    protected fun disconnect() {
        Timber.d("$TAG_CALL disconnect")
        if (isDisconnected.compareAndSet(false, true)) {
            Timber.d("$TAG_CALL real disconnect")
            stopForeground(true)
            callState.reset()
            callDebugState.reset()
            audioManager.reset()
            pipCallView.close()
            peerConnectionClient.dispose()
            timeoutFuture?.cancel(true)

            onCallDisconnected()
        }
    }

    protected fun initWebRtc() {
        if (::peerConnectionClient.isInitialized && ::audioManager.isInitialized) {
            return
        }

        peerConnectionClient = PeerConnectionClient(MixinApplication.appContext, this)
        callExecutor.execute {
            peerConnectionClient.createPeerConnectionFactory(PeerConnectionFactory.Options())
        }
        audioManager = CallAudioManager(
            this, audioSwitch,
            object : CallAudioManager.Callback {
                override fun customAudioDeviceAvailable(available: Boolean) {
                    callState.customAudioDeviceAvailable = available
                }
            }
        )
    }

    abstract fun needInitWebRtc(action: String): Boolean
    abstract fun handleIntent(intent: Intent): Boolean
    abstract fun onCallDisconnected()
    abstract fun onDestroyed()
    abstract fun onTimeout()
    abstract fun onTurnServerError()
    abstract fun handleLocalEnd()

    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {
    }

    override fun onIceConnected() {
    }

    override fun onConnected() {
        callExecutor.execute { handleConnected() }
    }

    override fun onDisconnected() {
        if (callState.isConnected()) {
            callState.disconnected = true
        }
    }

    override fun onClosed() {
    }

    override fun onPeerConnectionStatsReady(reports: Array<StatsReport>) {
    }

    override fun onPeerConnectionClosed() {
    }

    override fun onIceDisconnected() {
    }

    override fun onPeerConnectionError(errorMsg: String) {
        val callAudioMsg = audioManager.getMsg()
        val fullMsg = StringBuilder().append(errorMsg).appendLine().append(callAudioMsg).toString()
        reportException(IllegalStateException(fullMsg))
    }

    private fun handleConnected() {
        Timber.d("$TAG_CALL callState: ${callState.state}")
        if (callState.isIdle()) return

        val refreshState = !callState.isConnected()
        if (refreshState) {
            val connectedTime = System.currentTimeMillis()
            callState.connectedTime = connectedTime
            callState.state = CallState.STATE_CONNECTED
            updateForegroundNotification()
            heavyClickVibrate()
            audioManager.stop()
            pipCallView.startTimer(connectedTime)
        }
        timeoutFuture?.cancel(true)

        peerConnectionClient.setAudioEnable(callState.audioEnable)
        peerConnectionClient.enableCommunication()
        callState.disconnected = false
        callState.reconnecting = false

        lifecycleScope.launch(observeStatsDispatcher) {
            peerConnectionClient.observeStats {
                pipCallView.shown
            }
        }
    }

    private fun handleMuteAudio(intent: Intent) {
        if (callState.isIdle()) return
        val extras = intent.extras ?: return

        val enable = !extras.getBoolean(EXTRA_MUTE)
        callState.audioEnable = enable
        peerConnectionClient.setAudioEnable(enable)
    }

    private fun handleSpeakerphone(intent: Intent) {
        if (callState.isIdle()) return
        val extras = intent.extras ?: return

        val speakerphone = extras.getBoolean(EXTRA_SPEAKERPHONE)
        callState.speakerEnable = speakerphone
        audioManager.isSpeakerOn = speakerphone
    }

    private fun handleLogCallState() {
        if (callState.isIdle()) return

        val pcMsg = peerConnectionClient.getPCMessage()
        val callAudioMsg = audioManager.getMsg()
        val fullMsg = StringBuilder().append(pcMsg).appendLine().append(callAudioMsg).toString()
        Timber.e(fullMsg)
    }

    private fun handleCheckTimeout() {
        if (callState.isIdle() || callState.isConnected()) return

        onTimeout()
    }

    protected fun updateForegroundNotification() {
        if (isDisconnected.get() || isDestroyed.get()) return

        supportsOreo {
            val channel = NotificationChannel(
                BlazeMessageService.CHANNEL_NODE,
                MixinApplication.get().getString(R.string.Messaging_Node),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.lockscreenVisibility = Notification.VISIBILITY_SECRET
            channel.setSound(null, null)
            channel.setShowBadge(false)
            supportsOreo {
                ChannelManager.createNodeChannel(notificationManager)
            }
            CallNotificationBuilder.getCallNotification(this, callState)?.let {
                startForeground(CallNotificationBuilder.WEBRTC_NOTIFICATION, it)
            }
        }
    }

    protected fun getTurnServer(action: (List<PeerConnection.IceServer>) -> Unit) = runBlocking {
        handleMixinResponse(
            invokeNetwork = {
                accountService.getTurn()
            },
            successBlock = {
                val array = it.data as Array<TurnServer>
                action.invoke(genIceServerList(array))
            },
            exceptionBlock = {
                handleFetchTurnError(it.message)
                return@handleMixinResponse false
            },
            failureBlock = {
                handleFetchTurnError(it.error?.toString())
                return@handleMixinResponse true
            }
        )
    }

    private fun handleFetchTurnError(message: String?) {
        val error = "$TAG_CALL handleFetchTurnError $message"
        Timber.w(error)
        reportException(IllegalStateException(error))
        callExecutor.execute { onTurnServerError() }
    }

    private fun genIceServerList(array: Array<TurnServer>): List<PeerConnection.IceServer> {
        val iceServer = arrayListOf<PeerConnection.IceServer>()
        array.forEach {
            iceServer.add(
                PeerConnection.IceServer.builder(it.url)
                    .setUsername(it.username)
                    .setPassword(it.credential)
                    .createIceServer()
            )
        }
        return iceServer
    }

    protected fun checkConversation(message: Message): Boolean {
        val conversation = conversationRepo.getConversation(message.conversationId)
        if (conversation != null) return true

        return conversationRepo.refreshConversation(message.conversationId)
    }

    enum class CallState {
        STATE_IDLE, STATE_DIALING, STATE_RINGING, STATE_ANSWERING, STATE_CONNECTED, STATE_BUSY
    }

    inner class TimeoutRunnable : Runnable {
        override fun run() {
            handleCheckTimeout()
        }
    }

    companion object {
        const val TAG = "CallService"
    }
}

const val TAG_CALL = "TAG_CALL"

const val DEFAULT_TIMEOUT_MINUTES = 1L
const val DEFAULT_IGNORE_MINUTES = 60L

const val ACTION_CALL_DISCONNECT = "call_disconnect"

const val ACTION_MUTE_AUDIO = "mute_audio"
const val ACTION_SPEAKERPHONE = "speakerphone"

const val ACTION_LOG_CALL_STATE = "report"

const val EXTRA_CONVERSATION_ID = "conversation_id"
const val EXTRA_USERS = "users"
const val EXTRA_USER_ID = "user_id"
const val EXTRA_BLAZE = "blaze"
const val EXTRA_MUTE = "mute"
const val EXTRA_SPEAKERPHONE = "speakerphone"
const val EXTRA_PENDING_CANDIDATES = "pending_candidates"

inline fun <reified T : CallService> muteAudio(ctx: Context, checked: Boolean) = startService<T>(ctx, ACTION_MUTE_AUDIO) {
    it.putExtra(EXTRA_MUTE, checked)
}

inline fun <reified T : CallService> speakerPhone(ctx: Context, checked: Boolean) = startService<T>(ctx, ACTION_SPEAKERPHONE) {
    it.putExtra(EXTRA_SPEAKERPHONE, checked)
}

inline fun <reified T : CallService> disconnect(ctx: Context) {
    startService<T>(ctx, ACTION_CALL_DISCONNECT) {}
}

inline fun <reified T : CallService> startService(
    ctx: Context,
    action: String? = null,
    putExtra: ((intent: Intent) -> Unit)
) {
    val intent = Intent(ctx, T::class.java).apply {
        this.action = action
        putExtra.invoke(this)
    }
    ctx.startService(intent)
}

inline fun <reified T : CallService> logCallState(ctx: Context) = startService<T>(ctx, ACTION_LOG_CALL_STATE) {}

fun anyCallServiceRunning(context: Context) = isVoiceCallServiceRunning(context) || isGroupCallServiceRunning(context)

fun isVoiceCallServiceRunning(context: Context) = context.isServiceRunning(VoiceCallService::class.java)

fun isGroupCallServiceRunning(context: Context) = context.isServiceRunning(GroupCallService::class.java)
