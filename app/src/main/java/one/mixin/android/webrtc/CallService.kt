package one.mixin.android.webrtc

import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import com.google.gson.Gson
import dagger.android.AndroidInjection
import kotlinx.coroutines.runBlocking
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.service.AccountService
import one.mixin.android.db.MixinDatabase
import one.mixin.android.di.type.DatabaseCategory
import one.mixin.android.di.type.DatabaseCategoryEnum
import one.mixin.android.extension.supportsOreo
import one.mixin.android.extension.vibrate
import one.mixin.android.job.MixinJobManager
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.ui.call.CallNotificationBuilder
import one.mixin.android.util.Session
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
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

abstract class CallService : LifecycleService(), PeerConnectionClient.PeerConnectionEvents {

    protected val callExecutor = Executors.newSingleThreadExecutor()
    protected val timeoutExecutor = Executors.newScheduledThreadPool(1)
    protected var timeoutFuture: ScheduledFuture<*>? = null

    protected val audioManager: CallAudioManager by lazy {
        CallAudioManager(this)
    }
    protected val peerConnectionClient: PeerConnectionClient by lazy {
        PeerConnectionClient(this, this)
    }

    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    @field:[DatabaseCategory(DatabaseCategoryEnum.BASE)]
    lateinit var database: MixinDatabase
    @Inject
    lateinit var accountService: AccountService
    @Inject
    lateinit var callState: CallStateLiveData
    @Inject
    lateinit var conversationRepo: ConversationRepository

    protected val gson = Gson()

    protected lateinit var self: User

    private var isDestroyed = AtomicBoolean(false)

    private val pipCallView by lazy {
        PipCallView.get()
    }

    private var isForeground = false

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
        peerConnectionClient.createPeerConnectionFactory(PeerConnectionFactory.Options())
        Session.getAccount()?.toUser().let { user ->
            if (user == null) {
                stopSelf()
            } else {
                self = user
            }
        }
        supportsOreo {
            updateForegroundNotification()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        isForeground = intent?.extras?.getBoolean(EXTRA_FOREGROUND) ?: true
        supportsOreo {
            updateForegroundNotification()
        }

        if (intent == null || intent.action == null || isDestroyed.get()) {
            return START_NOT_STICKY
        }

        callExecutor.execute {
            if (!handleIntent(intent)) {
                when (intent.action) {
                    ACTION_MUTE_AUDIO -> handleMuteAudio(intent)
                    ACTION_SPEAKERPHONE -> handleSpeakerphone(intent)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Timber.d("@@@ onDestroy")
        super.onDestroy()
        if (isDestroyed.compareAndSet(false, true)) {
            peerConnectionClient.release()

            onDestroyed()
        }
    }

    protected fun disconnect() {
        Timber.d("@@@ disconnect")
        if (isDestroyed.get()) return

        stopForeground(true)
        isForeground = false
        audioManager.release()
        pipCallView.close()
        callState.reset()
        peerConnectionClient.close()
        timeoutFuture?.cancel(true)

        callState.state = CallState.STATE_IDLE
        onCallDisconnected()
    }

    abstract fun handleIntent(intent: Intent): Boolean
    abstract fun handleCallLocalFailed()
    abstract fun handleCallCancel(intent: Intent? = null)
    abstract fun handleCallLocalEnd(intent: Intent? = null)
    abstract fun onCallDisconnected()
    abstract fun onDestroyed()

    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {
    }

    override fun onIceConnected() {
    }

    override fun onConnected() {
        callExecutor.execute { handleConnected() }
    }

    override fun onIceDisconnected() {
    }

    override fun onPeerConnectionStatsReady(reports: Array<StatsReport>) {
    }

    override fun onPeerConnectionError(description: String) {
        callExecutor.execute { handleCallLocalFailed() }
    }

    override fun onPeerConnectionClosed() {
    }

    private fun handleConnected() {
        if (callState.isConnected()) return

        val connectedTime = System.currentTimeMillis()
        callState.connectedTime = connectedTime
        callState.state = CallState.STATE_CONNECTED
        updateForegroundNotification()
        timeoutFuture?.cancel(true)
        vibrate(longArrayOf(0, 30))
        audioManager.stop()
        peerConnectionClient.setAudioEnable(callState.audioEnable)
        peerConnectionClient.enableCommunication()

        pipCallView.startTimer(connectedTime)
    }

    private fun handleMuteAudio(intent: Intent) {
        val extras = intent.extras ?: return

        val enable = !extras.getBoolean(EXTRA_MUTE)
        callState.audioEnable = enable
        peerConnectionClient.setAudioEnable(enable)
    }

    private fun handleSpeakerphone(intent: Intent) {
        val extras = intent.extras ?: return

        val speakerphone = extras.getBoolean(EXTRA_SPEAKERPHONE)
        callState.speakerEnable = speakerphone
        audioManager.isSpeakerOn = speakerphone
    }

    private fun handleCheckTimeout() {
        if (callState.isIdle() || callState.isConnected()) return

        handleCallCancel()
    }

    protected fun isBusy(): Boolean {
        val tm = getSystemService<TelephonyManager>()
        return callState.isNotIdle() || tm?.callState != TelephonyManager.CALL_STATE_IDLE
    }

    protected fun updateForegroundNotification() {
        if (!isForeground) return

        CallNotificationBuilder.getCallNotification(this, callState)?.let {
            startForeground(CallNotificationBuilder.WEBRTC_NOTIFICATION, it)
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
                handleFetchTurnError()
                return@handleMixinResponse false
            },
            failureBlock = {
                handleFetchTurnError()
                return@handleMixinResponse true
            }
        )
    }

    private fun handleFetchTurnError() {
        Timber.d("@@@ handleFetchTurnError")
        callExecutor.execute { handleCallLocalFailed() }
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

const val DEFAULT_TIMEOUT_MINUTES = 1L

const val ACTION_MUTE_AUDIO = "mute_audio"
const val ACTION_SPEAKERPHONE = "speakerphone"

const val EXTRA_CONVERSATION_ID = "conversation_id"
const val EXTRA_USERS = "users"
const val EXTRA_USER_ID = "user_id"
const val EXTRA_BLAZE = "blaze"
const val EXTRA_MUTE = "mute"
const val EXTRA_SPEAKERPHONE = "speakerphone"
const val EXTRA_PENDING_CANDIDATES = "pending_candidates"
const val EXTRA_FOREGROUND = "foreground"

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
    foreground: Boolean = true,
    putExtra: ((intent: Intent) -> Unit)
) {
    val intent = Intent(ctx, T::class.java).apply {
        this.action = action
        putExtra.invoke(this)
        putExtra(EXTRA_FOREGROUND, foreground)
    }
    if (foreground) {
        ContextCompat.startForegroundService(ctx, intent)
    } else {
        ctx.startService(intent)
    }
}

inline fun <reified T : CallService> stopService(context: Context) {
    context.stopService(Intent(context, T::class.java))
}
