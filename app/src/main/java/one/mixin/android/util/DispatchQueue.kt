package one.mixin.android.util

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log

import java.util.concurrent.CountDownLatch

class DispatchQueue(threadName: String) : Thread() {
    @Volatile
    private var handler: Handler? = null
    private val syncLatch = CountDownLatch(1)

    init {
        name = threadName
        start()
    }

    fun sendMessage(msg: Message, delay: Int = 0) {
        try {
            syncLatch.await()
            if (delay <= 0) {
                handler!!.sendMessage(msg)
            } else {
                handler!!.sendMessageDelayed(msg, delay.toLong())
            }
        } catch (e: Exception) {
            Log.e(TAG, "", e)
        }
    }

    fun cancelRunnable(runnable: Runnable) {
        try {
            syncLatch.await()
            handler!!.removeCallbacks(runnable)
        } catch (e: Exception) {
            Log.e(TAG, "", e)
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
            Log.e(TAG, "", e)
        }
    }

    fun cleanupQueue() {
        try {
            syncLatch.await()
            handler!!.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            Log.e(TAG, "", e)
        }
    }

    fun handleMessage(inputMessage: Message) {
    }

    override fun run() {
        Looper.prepare()
        handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                this@DispatchQueue.handleMessage(msg)
            }
        }
        syncLatch.countDown()
        Looper.loop()
    }

    companion object {
        private const val TAG = "DispatchQueue"
    }
}