package one.mixin.android.job

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.room.InvalidationTracker
import com.birbit.android.jobqueue.network.NetworkEventProvider
import com.birbit.android.jobqueue.network.NetworkUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.Constants.MARK_REMOTE_LIMIT
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.service.MessageService
import one.mixin.android.db.FloodMessageDao
import one.mixin.android.db.JobDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.RemoteMessageStatusDao
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.networkConnected
import one.mixin.android.extension.notificationManager
import one.mixin.android.extension.supportsOreo
import one.mixin.android.job.BaseJob.Companion.PRIORITY_ACK_MESSAGE
import one.mixin.android.receiver.ExitBroadcastReceiver
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BatteryOptimizationDialogActivity
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.ChannelManager.Companion.createNodeChannel
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.reportException
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.createAckJob
import one.mixin.android.websocket.ACKNOWLEDGE_MESSAGE_RECEIPTS
import one.mixin.android.websocket.BlazeAckMessage
import one.mixin.android.websocket.BlazeMessageData
import one.mixin.android.websocket.CREATE_MESSAGE
import one.mixin.android.websocket.ChatWebSocket
import one.mixin.android.websocket.PlainDataAction
import one.mixin.android.websocket.PlainJsonMessagePayload
import one.mixin.android.websocket.createParamBlazeMessage
import one.mixin.android.websocket.createPlainJsonParam
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BlazeMessageService : LifecycleService(), NetworkEventProvider.Listener, ChatWebSocket.WebSocketObserver {

    companion object {
        const val CHANNEL_NODE = "channel_node"
        const val FOREGROUND_ID = 666666
        const val ACTION_TO_BACKGROUND = "action_to_background"
        const val ACTION_ACTIVITY_RESUME = "action_activity_resume"
        const val ACTION_ACTIVITY_PAUSE = "action_activity_pause"

        fun startService(ctx: Context, action: String? = null) {
            val intent = Intent(ctx, BlazeMessageService::class.java).apply {
                this.action = action
            }
            ctx.startService(intent)
        }

        fun stopService(ctx: Context) {
            val intent = Intent(ctx, BlazeMessageService::class.java)
            ctx.stopService(intent)
        }
    }

    @Inject
    lateinit var networkUtil: JobNetworkUtil
    @Inject
    lateinit var database: MixinDatabase
    @Inject
    lateinit var webSocket: ChatWebSocket
    @Inject
    lateinit var floodMessageDao: FloodMessageDao
    @Inject
    lateinit var remoteMessageStatusDao: RemoteMessageStatusDao
    @Inject
    lateinit var participantDao: ParticipantDao
    @Inject
    lateinit var jobDao: JobDao
    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    lateinit var callState: CallStateLiveData
    @Inject
    lateinit var messageService: MessageService

    private val accountId = Session.getAccountId()
    private val gson = GsonHelper.customGson

    private val powerManager by lazy { getSystemService<PowerManager>() }
    private var isIgnoringBatteryOptimizations = false

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onCreate() {
        super.onCreate()
        webSocket.setWebSocketObserver(this)
        webSocket.connect()
        startFloodJob()
        startAckJob()
        startStatusJob()
        networkUtil.setListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        updateIgnoringBatteryOptimizations()

        if (intent == null) return START_STICKY

        if (intent.action == ACTION_TO_BACKGROUND) {
            stopForeground(true)
            if (!isIgnoringBatteryOptimizations) {
                BatteryOptimizationDialogActivity.show(this, true)
            }
            return START_STICKY
        }

        if (!isIgnoringBatteryOptimizations) {
            setForegroundIfNecessary()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAckJob()
        stopFloodJob()
        stopStatusJob()
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

    private fun updateIgnoringBatteryOptimizations() {
        isIgnoringBatteryOptimizations = powerManager?.isIgnoringBatteryOptimizations(packageName) ?: false
    }

    @SuppressLint("NewApi")
    private fun setForegroundIfNecessary() {
        val exitIntent = Intent(this, ExitBroadcastReceiver::class.java).apply {
            action = ACTION_TO_BACKGROUND
        }
        val exitPendingIntent = PendingIntent.getBroadcast(
            this, 0, exitIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_NODE)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.background_connection_enabled))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setWhen(0)
            .setDefaults(0)
            .setSound(null)
            .setDefaults(0)
            .setOnlyAlertOnce(true)
            .setColor(ContextCompat.getColor(this, R.color.colorLightBlue))
            .setSmallIcon(R.drawable.ic_msg_default)
            .addAction(R.drawable.ic_close_black, getString(R.string.action_exit), exitPendingIntent)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, MainActivity.getWakeUpIntent(this),
            PendingIntent.FLAG_IMMUTABLE
        )

        builder.setContentIntent(pendingIntent)

        supportsOreo {
            createNodeChannel(notificationManager)
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
            Timber.e(e)
        }
    }

    private var lastAckPendingCount = 0
    private tailrec suspend fun processAck(): Boolean {
        val ackMessages = jobDao.findAckJobs()
        if (ackMessages.isEmpty()) {
            return false
        } else if (ackMessages.size == 100) {
            jobDao.getJobsCount().apply {
                if (this >= 10000 && this - lastAckPendingCount >= 10000) {
                    lastAckPendingCount = this
                    reportException("ack job count: $this", Exception())
                }
            }
        }
        try {
            messageService.acknowledgements(ackMessages.map { gson.fromJson(it.blazeMessage, BlazeAckMessage::class.java) })
            jobDao.deleteList(ackMessages)
        } catch (e: Exception) {
            Timber.e(e, "Send ack exception")
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
                    ackMessages = it
                )
            )
            val encoded = plainText.toByteArray().base64Encode()
            val bm = createParamBlazeMessage(createPlainJsonParam(participantDao.joinedConversationId(accountId), accountId, encoded, sessionId))
            jobManager.addJobInBackground(SendPlaintextJob(bm, PRIORITY_ACK_MESSAGE))
            jobDao.deleteList(jobs)
        }
    }

    private val messageDecrypt by lazy { DecryptMessage(lifecycleScope) }
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
                Timber.e(e)
                runFloodJob()
            }
        }
    }

    private tailrec suspend fun processFloodMessage(): Boolean {
        val messages = floodMessageDao.findFloodMessages()
        return if (!messages.isNullOrEmpty()) {
            messages.forEach { message ->
                val data = gson.fromJson(message.data, BlazeMessageData::class.java)
                if (data.category.startsWith("WEBRTC_") || data.category.startsWith("KRAKEN_")) {
                    callMessageDecrypt.onRun(data)
                } else {
                    messageDecrypt.onRun(data)
                }
                floodMessageDao.delete(message)
            }
            processFloodMessage()
        } else {
            false
        }
    }

    private fun startStatusJob() {
        database.invalidationTracker.addObserver(statusObserver)
    }

    private fun stopStatusJob() {
        database.invalidationTracker.removeObserver(statusObserver)
    }

    private var statusJob: Job? = null
    private val statusObserver = object : InvalidationTracker.Observer("remote_messages_status") {
        override fun onInvalidated(tables: MutableSet<String>) {
            runStatusJob()
        }
    }

    @Synchronized
    private fun runStatusJob() {
        try {
            if (statusJob?.isActive == true) {
                return
            }
            statusJob = lifecycleScope.launch(Dispatchers.IO) {
                processStatus()
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun processStatus(): Boolean {
        val list = remoteMessageStatusDao.findRemoteMessageStatus()
        if (list.isEmpty()) {
            return false
        }
        list.map { msg ->
            createAckJob(
                ACKNOWLEDGE_MESSAGE_RECEIPTS,
                BlazeAckMessage(msg.messageId, MessageStatus.READ.name)
            )
        }.apply {
            database.jobDao().insertList(this)
        }
        Session.getExtensionSessionId()?.let { _ ->
            val conversationId = list.first().conversationId
            list.map { msg ->
                createAckJob(CREATE_MESSAGE, BlazeAckMessage(msg.messageId, MessageStatus.READ.name), conversationId)
            }.let { jobs ->
                database.jobDao().insertList(jobs)
            }
        }
        remoteMessageStatusDao.deleteList(list)
        return if (list.size >= MARK_REMOTE_LIMIT) {
            processStatus()
        } else {
            true
        }
    }
}
