package one.mixin.android.util

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.os.Message
import timber.log.Timber
import java.util.concurrent.CountDownLatch

class DispatchQueue(threadName: String) : Thread() {
    @Volatile
    private var handler: Handler? = null
    private val syncLatch = CountDownLatch(1)

    init {
        name = threadName
        start()
    }

    @Suppress("unused")
    fun sendMessage(msg: Message, delay: Int = 0) {
        try {
            syncLatch.await()
            if (delay <= 0) {
                handler!!.sendMessage(msg)
            } else {
                handler!!.sendMessageDelayed(msg, delay.toLong())
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun cancelRunnable(runnable: Runnable) {
        try {
            syncLatch.await()
            handler!!.removeCallbacks(runnable)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    @JvmOverloads
    fun postRunnable(runnable: Runnable, delay: Long = 0) {
        try {
            syncLatch.await()
            if (delay <= 0) {
                handler!!.post(runnable)
            } else {
                handler!!.postDelayed(runnable, delay)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    @Suppress("unused")
    fun cleanupQueue() {
        try {
            syncLatch.await()
            handler!!.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun handleMessage(@Suppress("UNUSED_PARAMETER") inputMessage: Message) {
    }

    override fun run() {
        Looper.prepare()
        handler = @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(msg: Message) {
                this@DispatchQueue.handleMessage(msg)
            }
        }
        syncLatch.countDown()
        Looper.loop()
    }
}
