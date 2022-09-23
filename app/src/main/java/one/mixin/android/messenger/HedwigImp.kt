package one.mixin.android.messenger

import androidx.room.InvalidationTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.db.cache.CacheDataBase
import one.mixin.android.job.DecryptCallMessage
import one.mixin.android.job.DecryptMessage
import one.mixin.android.job.pendingMessageStatusMap
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.CallStateLiveData
import one.mixin.android.websocket.BlazeMessageData
import timber.log.Timber

class HedwigImp(
    private val cacheDataBase: CacheDataBase,
    private val callState: CallStateLiveData,
    private val lifecycleScope: CoroutineScope
) : Hedwig {

    override fun takeOff() {
        startObserveFlood()
    }

    override fun land() {
        stopObserveFlood()
        floodJob?.cancel()
    }

    private var floodJob: Job? = null
    private val floodObserver = object : InvalidationTracker.Observer("flood_messages") {
        override fun onInvalidated(tables: MutableSet<String>) {
            runFloodJob()
        }
    }

    private fun startObserveFlood() {
        cacheDataBase.invalidationTracker.addObserver(floodObserver)
    }

    private fun stopObserveFlood() {
        cacheDataBase.invalidationTracker.removeObserver(floodObserver)
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

    private val floodMessageDao by lazy {
        cacheDataBase.floodMessageDao()
    }

    private val gson by lazy {
        GsonHelper.customGson
    }

    private val messageDecrypt by lazy { DecryptMessage(lifecycleScope) }
    private val callMessageDecrypt by lazy { DecryptCallMessage(callState, lifecycleScope) }

    private tailrec suspend fun processFloodMessage(): Boolean {
        val messages = floodMessageDao.findFloodMessages()
        return if (messages.isNotEmpty()) {
            messages.forEach { message ->
                val data = gson.fromJson(message.data, BlazeMessageData::class.java)
                if (data.category.startsWith("WEBRTC_") || data.category.startsWith("KRAKEN_")) {
                    callMessageDecrypt.onRun(data)
                } else {
                    messageDecrypt.onRun(data)
                }
                floodMessageDao.delete(message)
                pendingMessageStatusMap.remove(data.messageId)
            }
            processFloodMessage()
        } else {
            false
        }
    }
}
