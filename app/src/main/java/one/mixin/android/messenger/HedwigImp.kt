package one.mixin.android.messenger

import androidx.room.InvalidationTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.pending.PendingDatabase
import one.mixin.android.job.DecryptCallMessage
import one.mixin.android.job.DecryptMessage
import one.mixin.android.job.pendingMessageStatusMap
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.chat.InvalidateFlow
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.RemoteMessageStatus
import one.mixin.android.vo.isMine
import one.mixin.android.websocket.BlazeMessageData
import timber.log.Timber

class HedwigImp(
    private val mixinDatabase: MixinDatabase,
    private val pendingDatabase: PendingDatabase,
    private val callState: CallStateLiveData,
    private val lifecycleScope: CoroutineScope,
) : Hedwig {

    override fun takeOff() {
        startObserveFlood()
        startObservePending()
    }

    override fun land() {
        stopObserveFlood()
        stopObservePending()
        floodJob?.cancel()
        pendingJob?.cancel()
    }

    private val messageDao by lazy {
        mixinDatabase.messageDao()
    }

    private val conversationDao by lazy {
        mixinDatabase.conversationDao()
    }

    private val conversationExtDao by lazy {
        mixinDatabase.conversationExtDao()
    }

    private val remoteMessageStatusDao by lazy {
        mixinDatabase.remoteMessageStatusDao()
    }

    private var floodJob: Job? = null
    private val floodObserver = object : InvalidationTracker.Observer("flood_messages") {
        override fun onInvalidated(tables: MutableSet<String>) {
            runFloodJob()
        }
    }

    private fun startObserveFlood() {
        runFloodJob()
        pendingDatabase.addObserver(floodObserver)
    }

    private fun stopObserveFlood() {
        pendingDatabase.removeObserver(floodObserver)
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

    private val gson by lazy {
        GsonHelper.customGson
    }

    private val messageDecrypt by lazy { DecryptMessage(lifecycleScope) }
    private val callMessageDecrypt by lazy { DecryptCallMessage(callState, lifecycleScope) }

    private tailrec suspend fun processFloodMessage(): Boolean {
        val messages = pendingDatabase.findFloodMessages()
        return if (messages.isNotEmpty()) {
            messages.forEach { message ->
                val data = gson.fromJson(message.data, BlazeMessageData::class.java)
                if (data.category.startsWith("WEBRTC_") || data.category.startsWith("KRAKEN_")) {
                    callMessageDecrypt.onRun(data)
                } else {
                    messageDecrypt.onRun(data)
                }
                pendingDatabase.deleteFloodMessage(message)
                pendingMessageStatusMap.remove(data.messageId)
            }
            processFloodMessage()
        } else {
            false
        }
    }

    private var pendingJob: Job? = null
    private val pendingObserver = object : InvalidationTracker.Observer("pending_messages") {
        override fun onInvalidated(tables: MutableSet<String>) {
            runPendingJob()
        }
    }

    private fun startObservePending() {
        runPendingJob()
        pendingDatabase.addObserver(pendingObserver)
    }

    private fun stopObservePending() {
        pendingDatabase.removeObserver(pendingObserver)
    }

    @Synchronized
    private fun runPendingJob() {
        if (pendingJob?.isActive == true) {
            return
        }
        pendingJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val list = pendingDatabase.getPendingMessages()
                messageDao.insertList(list)
                pendingDatabase.deletePendingMessageByIds(list.map { it.messageId })
                list.groupBy { it.conversationId }.forEach { (conversationId, messages) ->
                    conversationExtDao.increment(conversationId, messages.size)
                    messages.filter { message ->
                        !message.isMine() && message.status != MessageStatus.READ.name
                    }.map { message ->
                        RemoteMessageStatus(
                            message.messageId,
                            message.conversationId,
                            MessageStatus.DELIVERED.name
                        )
                    }.let { remoteMessageStatus ->
                        remoteMessageStatusDao.insertList(remoteMessageStatus)
                    }
                    messages.last().let { message ->
                        conversationDao.updateLastMessageId(message.messageId, message.createdAt, message.conversationId)
                    }
                    remoteMessageStatusDao.updateConversationUnseen(conversationId)
                    InvalidateFlow.emit(conversationId)
                }

                if (list.size == 100) {
                    runPendingJob()
                }
            } catch (e: Exception) {
                Timber.e(e)
                runPendingJob()
            }
        }
    }
}
