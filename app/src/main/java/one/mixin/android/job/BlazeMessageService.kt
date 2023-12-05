package one.mixin.android.job

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.room.InvalidationTracker
import com.birbit.android.jobqueue.network.NetworkEventProvider
import com.birbit.android.jobqueue.network.NetworkUtil
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.DB_EXPIRED_LIMIT
import one.mixin.android.Constants.MARK_REMOTE_LIMIT
import one.mixin.android.Constants.TEAM_MIXIN_USER_ID
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.api.service.CircleService
import one.mixin.android.api.service.ConversationService
import one.mixin.android.api.service.MessageService
import one.mixin.android.db.ExpiredMessageDao
import one.mixin.android.db.JobDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.ParticipantDao
import one.mixin.android.db.RemoteMessageStatusDao
import one.mixin.android.db.TranscriptMessageDao
import one.mixin.android.db.deleteMessageById
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.db.flow.MessageFlow.ANY_ID
import one.mixin.android.db.pending.PendingDatabase
import one.mixin.android.event.ExpiredEvent
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.currentTimeSeconds
import one.mixin.android.extension.networkConnected
import one.mixin.android.extension.notificationManager
import one.mixin.android.extension.supportsOreo
import one.mixin.android.fts.FtsDatabase
import one.mixin.android.fts.deleteByMessageId
import one.mixin.android.job.BaseJob.Companion.PRIORITY_ACK_MESSAGE
import one.mixin.android.job.NotificationGenerator.conversationDao
import one.mixin.android.job.NotificationGenerator.conversationExtDao
import one.mixin.android.messenger.Hedwig
import one.mixin.android.messenger.HedwigImp
import one.mixin.android.receiver.ExitBroadcastReceiver
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BatteryOptimizationDialogActivity
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.util.ChannelManager.Companion.createNodeChannel
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.RomUtil
import one.mixin.android.util.reportException
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.absolutePath
import one.mixin.android.vo.createAckJob
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.isTranscript
import one.mixin.android.websocket.ACKNOWLEDGE_MESSAGE_RECEIPTS
import one.mixin.android.websocket.BlazeAckMessage
import one.mixin.android.websocket.CREATE_MESSAGE
import one.mixin.android.websocket.ChatWebSocket
import one.mixin.android.websocket.PlainDataAction
import one.mixin.android.websocket.PlainJsonMessagePayload
import one.mixin.android.websocket.createParamBlazeMessage
import one.mixin.android.websocket.createPlainJsonParam
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class BlazeMessageService : LifecycleService(), NetworkEventProvider.Listener, ChatWebSocket.WebSocketObserver {
    companion object {
        const val CHANNEL_NODE = "channel_node"
        const val FOREGROUND_ID = 666666
        const val ACTION_TO_BACKGROUND = "mixin.intent.action.TO_BACKGROUND"
        const val ACTION_ACTIVITY_RESUME = "action_activity_resume"
        const val ACTION_ACTIVITY_PAUSE = "action_activity_pause"

        fun startService(
            ctx: Context,
            action: String? = null,
        ) {
            val intent =
                Intent(ctx, BlazeMessageService::class.java).apply {
                    this.action = action
                }
            try {
                ctx.startService(intent)
            } catch (e: Exception) {
                reportException(IllegalStateException("Can't start service, action:$action, ${e.message}"))
            }
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
    lateinit var ftsDatabase: FtsDatabase

    @Inject
    lateinit var pendingDatabase: PendingDatabase

    @Inject
    lateinit var webSocket: ChatWebSocket

    @Inject
    lateinit var messageDao: MessageDao

    @Inject
    lateinit var transcriptMessageDao: TranscriptMessageDao

    @Inject
    lateinit var remoteMessageStatusDao: RemoteMessageStatusDao

    @Inject
    lateinit var expiredMessageDao: ExpiredMessageDao

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

    @Inject
    lateinit var conversationService: ConversationService

    @Inject
    lateinit var circleService: CircleService

    private val accountId = Session.getAccountId()
    private val gson = GsonHelper.customGson

    private val powerManager by lazy { getSystemService<PowerManager>() }
    private val activityManager by lazy { getSystemService<ActivityManager>() }
    private var isIgnoringBatteryOptimizations = false
    private var disposable: Disposable? = null
    private val destroyScope = scope(Lifecycle.Event.ON_DESTROY)

    private val hedwig: Hedwig by lazy {
        HedwigImp(database, pendingDatabase, conversationService, circleService, jobManager, callState, lifecycleScope)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    @SuppressLint("AutoDispose")
    override fun onCreate() {
        super.onCreate()
        webSocket.setWebSocketObserver(this)
        webSocket.connect()
        hedwig.takeOff()
        startObserveAck()
        startObserveStatus()
        startObserveExpired()
        runExpiredJob()
        networkUtil.setListener(this)
        if (disposable == null) {
            disposable =
                RxBus.listen(ExpiredEvent::class.java).observeOn(Schedulers.io())
                    .autoDispose(destroyScope)
                    .subscribe { event ->
                        val expiredIn = event.expireIn
                        if (expiredIn != null) {
                            val currentTime = currentTimeSeconds()
                            if (expiredMessageDao.markRead(event.messageId, currentTime) > 0) {
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        startExpiredJob(currentTime + expiredIn)
                                    }
                                }
                            } else {
                                expiredMessageDao.getExpiredMessageById(event.messageId)?.expireAt?.let { expiredAt ->
                                    startExpiredJob(expiredAt)
                                }
                            }
                        } else {
                            val expiredAt = requireNotNull(event.expireAt)
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    startExpiredJob(expiredAt)
                                }
                            }
                        }
                    }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)
        updateIgnoringBatteryOptimizations()

        if (intent == null) return START_STICKY

        if (intent.action == ACTION_TO_BACKGROUND) {
            stopForeground(STOP_FOREGROUND_REMOVE)
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
        hedwig.land()
        stopObserveAck()
        stopObserveStatus()
        stopObserveExpired()
        ackJob?.cancel()
        ackJob = null
        statusJob?.cancel()
        statusJob = null
        expiredJob?.cancel()
        expiredJob = null
        webSocket.disconnect()
        webSocket.setWebSocketObserver(null)
        networkUtil.unregisterListener()
        disposable?.dispose()
        disposable = null
    }

    override fun onNetworkChange(networkStatus: Int) {
        if (networkStatus != NetworkUtil.DISCONNECTED && MixinApplication.get().isOnline.get()) {
            webSocket.connect()
        }
    }

    override fun onSocketClose() {
    }

    override fun onSocketOpen() {
        runAckJob()
    }

    private fun updateIgnoringBatteryOptimizations() {
        isIgnoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !RomUtil.isEmui) {
            activityManager?.isBackgroundRestricted?.not()
        } else {
            powerManager?.isIgnoringBatteryOptimizations(packageName)
        } ?: false
    }

    @SuppressLint("NewApi")
    private fun setForegroundIfNecessary() {
        val exitIntent =
            Intent(this, ExitBroadcastReceiver::class.java).apply {
                action = ACTION_TO_BACKGROUND
            }
        val exitPendingIntent =
            PendingIntent.getBroadcast(
                this,
                0,
                exitIntent,
                PendingIntent.FLAG_IMMUTABLE,
            )

        val builder =
            NotificationCompat.Builder(this, CHANNEL_NODE)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.Messaging_node_running))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setWhen(0)
                .setDefaults(0)
                .setSound(null)
                .setDefaults(0)
                .setOnlyAlertOnce(true)
                .setColor(ContextCompat.getColor(this, R.color.colorLightBlue))
                .setSmallIcon(R.drawable.ic_msg_default)
                .addAction(R.drawable.ic_close_black, getString(R.string.Exit), exitPendingIntent)
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                MainActivity.getWakeUpIntent(this),
                PendingIntent.FLAG_IMMUTABLE,
            )

        builder.setContentIntent(pendingIntent)

        supportsOreo {
            createNodeChannel(notificationManager)
        }

        try {
            ServiceCompat.startForeground(this, FOREGROUND_ID, builder.build(), FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } catch (e: Exception) {
            reportException(e)
            Timber.e(e)
        }
    }

    private fun startObserveAck() {
        pendingDatabase.addObserver(ackObserver)
    }

    private fun stopObserveAck() {
        pendingDatabase.removeObserver(ackObserver)
    }

    private var ackJob: Job? = null
    private val ackObserver =
        object : InvalidationTracker.Observer("jobs") {
            override fun onInvalidated(tables: Set<String>) {
                runAckJob()
            }
        }

    @Synchronized
    private fun runAckJob() {
        try {
            if (ackJob?.isActive == true || !networkConnected()) {
                return
            }
            ackJob =
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        processAck()
                        Session.getExtensionSessionId()?.let {
                            syncMessageStatusToExtension(it)
                        }
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
            messageService.acknowledgements(
                ackMessages.map {
                    gson.fromJson(
                        it.blazeMessage,
                        BlazeAckMessage::class.java,
                    )
                },
            )
            jobDao.deleteList(ackMessages)
        } catch (e: Exception) {
            Timber.e(e, "Send ack exception")
        }
        ackJob?.ensureActive()
        return processAck()
    }

    private suspend fun syncMessageStatusToExtension(sessionId: String) {
        val jobs = jobDao.findCreateMessageJobs()
        if (jobs.isEmpty() || accountId == null) {
            return
        }
        jobs.map { gson.fromJson(it.blazeMessage, BlazeAckMessage::class.java) }.let {
            val plainText =
                gson.toJson(
                    PlainJsonMessagePayload(
                        action = PlainDataAction.ACKNOWLEDGE_MESSAGE_RECEIPTS.name,
                        ackMessages = it,
                    ),
                )
            val encoded = plainText.toByteArray().base64Encode()
            val bm = createParamBlazeMessage(createPlainJsonParam(participantDao.joinedConversationId(accountId) ?: generateConversationId(accountId, TEAM_MIXIN_USER_ID), accountId, encoded, sessionId))
            jobManager.addJobInBackground(SendPlaintextJob(bm, PRIORITY_ACK_MESSAGE))
            jobDao.deleteList(jobs)
        }
    }

    private fun startObserveStatus() {
        database.invalidationTracker.addObserver(statusObserver)
    }

    private fun stopObserveStatus() {
        database.invalidationTracker.removeObserver(statusObserver)
    }

    private var statusJob: Job? = null
    private val statusObserver =
        object : InvalidationTracker.Observer("remote_messages_status") {
            override fun onInvalidated(tables: Set<String>) {
                runStatusJob()
            }
        }

    private fun startObserveExpired() {
        database.invalidationTracker.addObserver(expiredObserver)
    }

    private fun stopObserveExpired() {
        database.invalidationTracker.removeObserver(expiredObserver)
    }

    private var expiredJob: Job? = null
    private val expiredObserver =
        object : InvalidationTracker.Observer("expired_messages") {
            override fun onInvalidated(tables: Set<String>) {
                runExpiredJob()
            }
        }

    @Synchronized
    private fun runStatusJob() {
        try {
            if (statusJob?.isActive == true) {
                return
            }
            statusJob =
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        processStatus()
                    }
                }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private tailrec fun processStatus(): Boolean {
        val list = remoteMessageStatusDao.findRemoteMessageStatus()
        if (list.isEmpty()) {
            return false
        }
        list.map { msg ->
            createAckJob(
                ACKNOWLEDGE_MESSAGE_RECEIPTS,
                BlazeAckMessage(msg.messageId, MessageStatus.READ.name),
            )
        }.apply {
            pendingDatabase.insertJobs(this)
        }
        Session.getExtensionSessionId()?.let { _ ->
            val conversationId = list.first().conversationId
            list.map { msg ->
                createAckJob(CREATE_MESSAGE, BlazeAckMessage(msg.messageId, MessageStatus.READ.name, msg.expireAt), conversationId)
            }.let { jobs ->
                pendingDatabase.insertJobs(jobs)
            }
        }
        remoteMessageStatusDao.deleteByMessageIds(list.map { it.messageId })
        return if (list.size >= MARK_REMOTE_LIMIT) {
            statusJob?.ensureActive()
            processStatus()
        } else {
            true
        }
    }

    private fun runExpiredJob() {
        if (expiredJob?.isActive == true) {
            return
        }
        expiredJob =
            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        processExpiredMessage()
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    runExpiredJob()
                }
            }
    }

    private fun startExpiredJob(expiredTime: Long) {
        val nextExpirationTime = this.nextExpirationTime
        if (expiredTime <= currentTimeSeconds() || (nextExpirationTime != null && expiredTime < nextExpirationTime)) {
            expiredJob?.cancel()
            runExpiredJob()
        }
        runExpiredJob()
    }

    private var nextExpirationTime: Long? = null

    private tailrec suspend fun processExpiredMessage() {
        val messages =
            expiredMessageDao.getExpiredMessages(currentTimeSeconds(), DB_EXPIRED_LIMIT)
        if (messages.isEmpty()) {
            val firstExpiredMessage = expiredMessageDao.getFirstExpiredMessage()
            if (firstExpiredMessage == null) {
                nextExpirationTime = null
            } else {
                nextExpirationTime = firstExpiredMessage.expireAt
                val delayTime =
                    max(
                        requireNotNull(firstExpiredMessage.expireAt) * 1000 - System.currentTimeMillis(),
                        0,
                    )
                Timber.e("Expired job: delay $delayTime")
                delay(delayTime)
                expiredJob?.ensureActive()
                processExpiredMessage()
            }
        } else {
            val ids = messages.map { it.messageId }
            val cIds = messageDao.findConversationsByMessages(ids)
            ids.forEach { messageId ->
                val messageMedia = pendingDatabase.findMessageMediaById(messageId) ?: messageDao.findMessageMediaById(messageId)
                Timber.e("Expired job: delete messages ${messageMedia?.type} - ${messageMedia?.messageId}")
                messageMedia?.absolutePath(
                    MixinApplication.appContext,
                    messageMedia.conversationId,
                    messageMedia.mediaUrl,
                )?.let {
                    jobManager.addJobInBackground(AttachmentDeleteJob(it))
                }
                if (messageMedia?.isTranscript() == true) {
                    jobManager.addJobInBackground(TranscriptDeleteJob(listOf(messageId)))
                }
                pendingDatabase.deletePendingMessageById(messageId)
                database.deleteMessageById(messageId)
                ftsDatabase.deleteByMessageId(messageId)
                MessageFlow.delete(ANY_ID, messageId)
            }

            cIds.forEach { id ->
                conversationDao.refreshLastMessageId(id)
                conversationExtDao.refreshCountByConversationId(id)
            }
            nextExpirationTime = null
            expiredJob?.ensureActive()
            processExpiredMessage()
        }
    }
}
