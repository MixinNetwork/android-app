package one.mixin.android.util

import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.UserBatchProcessorJob
import timber.log.Timber
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
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            this.jobManager = jobManager
            scheduler = Executors.newSingleThreadScheduledExecutor()
            scheduler.scheduleAtFixedRate(
                ::checkAndProcess,
                MAX_INTERVAL_MINUTES,
                MAX_INTERVAL_MINUTES,
                TimeUnit.MINUTES,
            )
            isInitialized = true
            if (userSet.size >= MAX_SIZE) {
                processUsers()
            }
        }
    }

    fun addUser(userId: String) {
        userSet.add(userId)
        if (isInitialized && userSet.size >= MAX_SIZE) {
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
        if (!isInitialized || userSet.isEmpty()) return
        val userIds =
            synchronized(userSet) {
                if (userSet.isEmpty()) return
                val ids = userSet.toList()
                userSet.removeAll(ids.toSet())
                ids
            }
        try {
            jobManager.addJobInBackground(UserBatchProcessorJob(userIds))
            lastProcessTime = System.currentTimeMillis()
        } catch (e: Exception) {
            userSet.addAll(userIds)
            Timber.e(e)
        }
    }

    fun isInitialized(): Boolean = isInitialized

    @Synchronized
    fun shutdown() {
        if (::scheduler.isInitialized) {
            scheduler.shutdown()
        }
        synchronized(userSet) {
            userSet.clear()
        }
        synchronized(Companion) {
            instance = null
        }
        isInitialized = false
    }
}
