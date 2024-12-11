package one.mixin.android.util

import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.UserBatchProcessorJob
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class UserBatchProcessor private constructor() {
    private val userSet = ConcurrentHashMap.newKeySet<String>()
    private lateinit var scheduler: ScheduledExecutorService
    private var lastProcessTime = System.currentTimeMillis()
    private lateinit var jobManager: MixinJobManager

    companion object {
        private const val MAX_SIZE = 100
        private const val MAX_INTERVAL_MINUTES = 10L

        @Volatile
        private var instance: UserBatchProcessor? = null

        fun getInstance(): UserBatchProcessor {
            return instance ?: synchronized(this) {
                instance ?: UserBatchProcessor().also { instance = it }
            }
        }
    }

    @Volatile
    private var isInitialized = false

    fun init(jobManager: MixinJobManager) {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    this.jobManager = jobManager

                    if (!::scheduler.isInitialized) {
                        scheduler = Executors.newSingleThreadScheduledExecutor()
                    }

                    scheduler.scheduleAtFixedRate({
                        checkAndProcess()
                    }, 1, 10, TimeUnit.MINUTES)

                    isInitialized = true
                }
            }
        }
    }
    fun addUser(userId: String) {
        userSet.add(userId)
        if (userSet.size >= MAX_SIZE) {
            processUsers()
        }
    }

    private fun checkAndProcess() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessTime >= TimeUnit.MINUTES.toMillis(MAX_INTERVAL_MINUTES)) {
            processUsers()
        }
    }

    private fun processUsers() {
        if (userSet.isEmpty) return

        synchronized(userSet) {
            try {
                jobManager.addJobInBackground(UserBatchProcessorJob(userSet.toList()))
                userSet.clear()
                lastProcessTime = System.currentTimeMillis()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun isInitialized(): Boolean = isInitialized

    fun shutdown() {
        scheduler.shutdown()
        userSet.clear()
        instance = null
        isInitialized = false
    }
}