package one.mixin.android.job

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.service.MessageService
import one.mixin.android.db.FloodMessageDao
import one.mixin.android.db.JobDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.insertNoReplaceList
import one.mixin.android.db.runInTransaction
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.networkConnected
import one.mixin.android.extension.supportsOreo
import one.mixin.android.job.BaseJob.Companion.PRIORITY_ACK_MESSAGE
import one.mixin.android.receiver.ExitBroadcastReceiver
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BatteryOptimizationDialogActivity
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.MessageFts4Helper
import one.mixin.android.util.reportException
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.createAckJob
import one.mixin.android.vo.isFtsMessage
import one.mixin.android.websocket.ACKNOWLEDGE_MESSAGE_RECEIPTS
import one.mixin.android.websocket.BlazeAckMessage
import one.mixin.android.websocket.BlazeMessageData
import one.mixin.android.websocket.ChatWebSocket
import one.mixin.android.websocket.PlainDataAction
import one.mixin.android.websocket.PlainJsonMessagePayload
import one.mixin.android.websocket.createParamBlazeMessage
import one.mixin.android.websocket.createPlainJsonParam
import org.jetbrains.anko.notificationManager
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@AndroidEntryPoint
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
            .setColor(ContextCompat.getColor(this, R.color.colorLightBlue))
            .setSmallIcon(R.drawable.ic_msg_default)
            .addAction(R.drawable.ic_close_black, getString(R.string.exit), exitPendingIntent)

        val pendingIntent = PendingIntent.getActivity(this, 0, MainActivity.getWakeUpIntent(this), 0)
        builder.setContentIntent(pendingIntent)

        supportsOreo {
            val channel = NotificationChannel(
                CHANNEL_NODE,
                MixinApplication.get().getString(R.string.notification_node),
                NotificationManager.IMPORTANCE_LOW
            )
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

    @OptIn(ExperimentalTime::class)
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

    @OptIn(ExperimentalTime::class)
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

    @ExperimentalTime
    private tailrec suspend fun processFloodMessage(): Boolean {
        val messages = floodMessageDao.findFloodMessages()
        return if (!messages.isNullOrEmpty()) {
            Timber.d(
                "@@@ messages size: ${messages.size}, cost: ${measureTime {
                    val ids = messages.map { it.messageId }
                    val existsMessageIds = database.messageDao().findMessageIdsByIds(ids).toSet()
                    val existsHistories = database.messageHistoryDao().findMessageHistoryByIds(ids).toSet()
                    val union = existsMessageIds.union(existsMessageIds)
                    val candidates = messages.filter { union.contains(it.messageId).not() }

                    Timber.d(
                        "@@@ ack exists messages size: ${union.size}, cost: ${measureTime {
                            val jobs = existsHistories.map { createAckJob(ACKNOWLEDGE_MESSAGE_RECEIPTS, BlazeAckMessage(it, MessageStatus.DELIVERED.name)) }
                            database.jobDao().insertNoReplaceList(jobs)
                        }}"
                    )

                    Timber.d(
                        "@@@ candidates size: ${candidates.size}, cost: ${measureTime {
                            candidates.forEach { message ->
                                val data = gson.fromJson(message.data, BlazeMessageData::class.java)
                                Timber.d(
                                    "@@@ single message ${data.category} cost: ${measureTime {
                                        if (data.category.startsWith("WEBRTC_") || data.category.startsWith("KRAKEN_")) {
                                            callMessageDecrypt.onRun(data)
                                        } else {
                                            messageDecrypt.onRun(data)
                                        }
                                    }}"
                                )
                            }
                            val pendingMessages = messageDecrypt.pendingInsertMessages
                            Timber.d(
                                "@@@ insert message & fts, list size: ${pendingMessages.size}, cost: ${measureTime {
                                    runInTransaction {
                                        database.messageDao().insertList(pendingMessages)

                                        val updateUnseenConversationIds = mutableSetOf<String>()

                                        pendingMessages.forEach { m ->
                                            if (m.isFtsMessage()) {
                                                MessageFts4Helper.insertOrReplaceMessageFts4(m)
                                            }
                                            updateUnseenConversationIds.add(m.conversationId)
                                        }

                                        Timber.d(
                                            "@@@ unseen size: ${updateUnseenConversationIds.size}, cost: ${
                                            measureTime {
                                                updateUnseenConversationIds.forEach { cid ->
                                                    database.conversationDao()
                                                        .unseenMessageCount(cid, Session.getAccountId())
                                                }
                                            }
                                            }"
                                        )
                                    }
                                    messageDecrypt.pendingInsertMessages.clear()
                                }}"
                            )
                        }}"
                    )

                    Timber.d(
                        "@@@ flood delete list cost: ${measureTime {
                            floodMessageDao.deleteList(messages)
                        }}"
                    )
                }}"
            )

            processFloodMessage()
        } else {
            false
        }
    }
}
