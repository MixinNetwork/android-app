package one.mixin.android.job

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.room.InvalidationTracker
import com.birbit.android.jobqueue.network.NetworkEventProvider
import com.birbit.android.jobqueue.network.NetworkUtil
import dagger.android.AndroidInjection
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.service.MessageService
import one.mixin.android.db.FloodMessageDao
import one.mixin.android.db.JobDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.ParticipantDao
import one.mixin.android.di.type.DatabaseCategory
import one.mixin.android.di.type.DatabaseCategoryEnum
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.networkConnected
import one.mixin.android.extension.supportsOreo
import one.mixin.android.job.BaseJob.Companion.PRIORITY_ACK_MESSAGE
import one.mixin.android.receiver.ExitBroadcastReceiver
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.Session
import one.mixin.android.vo.CallState
import one.mixin.android.websocket.BlazeAckMessage
import one.mixin.android.websocket.BlazeMessageData
import one.mixin.android.websocket.ChatWebSocket
import one.mixin.android.websocket.PlainDataAction
import one.mixin.android.websocket.PlainJsonMessagePayload
import one.mixin.android.websocket.createParamBlazeMessage
import one.mixin.android.websocket.createPlainJsonParam
import org.jetbrains.anko.notificationManager

class BlazeMessageService : LifecycleService(), NetworkEventProvider.Listener, ChatWebSocket.WebSocketObserver {

    companion object {
        val TAG = BlazeMessageService::class.java.simpleName
        const val CHANNEL_NODE = "channel_node"
        const val FOREGROUND_ID = 666666
        const val ACTION_TO_BACKGROUND = "action_to_background"
        const val ACTION_ACTIVITY_RESUME = "action_activity_resume"
        const val ACTION_ACTIVITY_PAUSE = "action_activity_pause"

        fun startService(ctx: Context, action: String? = null) {
            val intent = Intent(ctx, BlazeMessageService::class.java).apply {
                this.action = action
            }
            ContextCompat.startForegroundService(ctx, intent)
        }

        fun stopService(ctx: Context) {
            val intent = Intent(ctx, BlazeMessageService::class.java)
            ctx.stopService(intent)
        }
    }

    @Inject
    lateinit var networkUtil: JobNetworkUtil
    @Inject
    @field:[DatabaseCategory(DatabaseCategoryEnum.BASE)]
    lateinit var database: MixinDatabase
    @Inject
    lateinit var webSocket: ChatWebSocket
    @Inject
    lateinit var floodMessageDao: FloodMessageDao
    @Inject
    lateinit var participantDao: ParticipantDao
    @Inject
    lateinit var jobDao: JobDao
    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    lateinit var callState: CallState
    @Inject
    lateinit var messageService: MessageService

    private val accountId = Session.getAccountId()
    private val gson = GsonHelper.customGson

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
        webSocket.setWebSocketObserver(this)
        webSocket.connect()
        startFloodJob()
        startAckJob()
        networkUtil.setListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent == null) return START_STICKY
        when (ACTION_TO_BACKGROUND) {
            intent.action -> {
                stopForeground(true)
                return START_STICKY
            }
        }
        setForegroundIfNecessary()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAckJob()
        stopFloodJob()
        webSocket.disconnect()
    }

    override fun onNetworkChange(networkStatus: Int) {
        if (networkStatus != NetworkUtil.DISCONNECTED && MixinApplication.get().onlining.get()) {
            webSocket.connect()
        }
    }

    override fun onSocketClose() {
    }

    override fun onSocketOpen() {
        runFloodJob()
        runAckJob()
    }

    @SuppressLint("NewApi")
    private fun setForegroundIfNecessary() {
        val exitIntent = Intent(this, ExitBroadcastReceiver::class.java).apply {
            action = ACTION_TO_BACKGROUND
        }
        val exitPendingIntent = PendingIntent.getBroadcast(this, 0, exitIntent, 0)

        val builder = NotificationCompat.Builder(this, CHANNEL_NODE)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.background_connection_enabled))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setWhen(0)
            .setDefaults(0)
            .setSound(null)
            .setDefaults(0)
            .setOnlyAlertOnce(true)
            .setColor(ContextCompat.getColor(this, R.color.gray_light))
            .setSmallIcon(R.drawable.ic_msg_default)
            .addAction(R.drawable.ic_close_black, getString(R.string.exit), exitPendingIntent)

        val pendingIntent = PendingIntent.getActivity(this, 0, MainActivity.getWakeUpIntent(this), 0)
        builder.setContentIntent(pendingIntent)

        supportsOreo {
            val channel = NotificationChannel(CHANNEL_NODE,
                MixinApplication.get().getString(R.string.notification_node), NotificationManager.IMPORTANCE_LOW)
            channel.lockscreenVisibility = Notification.VISIBILITY_SECRET
            channel.setSound(null, null)
            channel.setShowBadge(false)
            notificationManager.createNotificationChannel(channel)
        }
        startForeground(FOREGROUND_ID, builder.build())
    }

    private fun startAckJob() {
        database.invalidationTracker.addObserver(ackObserver)
    }

    private fun stopAckJob() {
        database.invalidationTracker.removeObserver(ackObserver)
    }

    private var ackJob: Job? = null
    private val ackObserver = object : InvalidationTracker.Observer("jobs") {
        override fun onInvalidated(tables: MutableSet<String>) {
            runAckJob()
        }
    }

    @Synchronized
    private fun runAckJob() {
        try {
            if (ackJob?.isActive == true || !networkConnected()) {
                return
            }
            ackJob = lifecycleScope.launch(Dispatchers.IO) {
                processAck()
                Session.getExtensionSessionId()?.let {
                    syncMessageStatusToExtension(it)
                }
            }
        } catch (e: Exception) {
        }
    }

    private tailrec suspend fun processAck(): Boolean {
        val ackMessages = jobDao.findAckJobs()
        if (ackMessages.isEmpty()) {
            return false
        }
        try {
            messageService.acknowledgements(ackMessages.map { gson.fromJson(it.blazeMessage, BlazeAckMessage::class.java) })
            jobDao.deleteListSuspend(ackMessages)
        } catch (e: Exception) {
            Log.e(BlazeMessageService.TAG, "Send ack exception", e)
        }
        return processAck()
    }

    private suspend fun syncMessageStatusToExtension(sessionId: String) {
        val jobs = jobDao.findCreateMessageJobs()
        if (jobs.isEmpty() || accountId == null) {
            return
        }
        jobs.map { gson.fromJson(it.blazeMessage, BlazeAckMessage::class.java) }.let {
            val plainText = gson.toJson(
                PlainJsonMessagePayload(
                    action = PlainDataAction.ACKNOWLEDGE_MESSAGE_RECEIPTS.name,
                    ackMessages = it)
            )
            val encoded = plainText.toByteArray().base64Encode()
            val bm = createParamBlazeMessage(createPlainJsonParam(participantDao.joinedConversationId(accountId), accountId, encoded, sessionId))
            jobManager.addJobInBackground(SendPlaintextJob(bm, PRIORITY_ACK_MESSAGE))
            jobDao.deleteList(jobs)
        }
    }

    private val messageDecrypt by lazy { DecryptMessage() }
    private val callMessageDecrypt by lazy { DecryptCallMessage(callState, lifecycleScope) }

    private fun startFloodJob() {
        database.invalidationTracker.addObserver(floodObserver)
    }

    private fun stopFloodJob() {
        database.invalidationTracker.removeObserver(floodObserver)
    }

    private var floodJob: Job? = null
    private val floodObserver = object : InvalidationTracker.Observer("flood_messages") {
        override fun onInvalidated(tables: MutableSet<String>) {
            runFloodJob()
        }
    }

    @Synchronized
    private fun runFloodJob() {
        if (floodJob?.isActive == true) {
            return
        }
        floodJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                processFloodMessage()
            } catch (e: Exception) {
                runFloodJob()
            }
        }
    }

    private tailrec suspend fun processFloodMessage(): Boolean {
        val messages = floodMessageDao.findFloodMessages()
        if (!messages.isNullOrEmpty()) {
            messages.forEach { message ->
                val data = gson.fromJson(message.data, BlazeMessageData::class.java)
                if (data.category.startsWith("WEBRTC_")) {
                    callMessageDecrypt.onRun(data)
                } else {
                    messageDecrypt.onRun(data)
                }
                floodMessageDao.delete(message)
            }
            return processFloodMessage()
        } else {
            return false
        }
    }
}
