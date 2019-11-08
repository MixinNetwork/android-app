package one.mixin.android.ui.landing

import androidx.collection.ArrayMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.service.AccountService
import one.mixin.android.api.service.SignalKeyService
import one.mixin.android.api.service.UserService
import one.mixin.android.crypto.db.SenderKeyDao
import one.mixin.android.crypto.db.SessionDao
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.crypto.vo.SenderKey
import one.mixin.android.extension.getDeviceId
import one.mixin.android.job.RefreshOneTimePreKeysJob
import javax.inject.Inject
import kotlin.math.abs

class LoadingViewModel @Inject internal
constructor(
    private val signalKeyService: SignalKeyService,
    private val accountService: AccountService,
    private val userService: UserService
) : ViewModel() {
    private val sessionDao: SessionDao =
        SignalDatabase.getDatabase(MixinApplication.appContext).sessionDao()

    private val senderKeyDao: SenderKeyDao =
        SignalDatabase.getDatabase(MixinApplication.appContext).senderKeyDao()

    suspend fun pushAsyncSignalKeys(): MixinResponse<Void> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val signalKeys = RefreshOneTimePreKeysJob.generateKeys()
        val response = signalKeyService.pushSignalKeys(signalKeys).await()
        val time = System.currentTimeMillis() - start
        if (time < 2000) {
            delay(time)
        }
        return@withContext response
    }

    suspend fun updateSignalSession() {
        withContext(Dispatchers.IO) {
            val sessions = sessionDao.syncGetSessionAddress()
            sessions?.let { list ->
                val userIds = list.map { it.address }
                val response = userService.fetchSessions(userIds)
                val sessionMap = ArrayMap<String, Int>()
                if (response.isSuccess) {
                    response.data?.asSequence()?.forEach { item ->
                        val deviceId = item.session_id.getDeviceId()
                        sessionMap[item.user_id] = deviceId
                    }
                }
                if (sessionMap.isEmpty) {
                    return@withContext
                }
                for (s in sessions) {
                    sessionMap[s.address]?.let { d ->
                        s.device = d
                    }
                }
                sessionDao.insertList(sessions)
                val senderKeys = senderKeyDao.syncGetSenderKeys()
                senderKeys?.forEach { key ->
                    val userId = key.senderId.substring(0, key.senderId.length - 2)
                    sessionMap[userId]?.let { d ->
                        senderKeyDao.insert(SenderKey(key.groupId, "$userId:$d", key.record))
                    }
                }
            }
        }
    }

    fun pingServer(callback: () -> Unit, elseCallBack: (e: Exception?) -> Unit): Job {
        return viewModelScope.launch {
            try {
                val response = withContext(coroutineContext + Dispatchers.IO) {
                    accountService.ping().execute()
                }
                response.headers()["X-Server-Time"]?.toLong()?.let { serverTime ->
                    if (abs(serverTime / 1000000 - System.currentTimeMillis()) < 600000L) { // 10 minutes
                        callback.invoke()
                    } else {
                        elseCallBack.invoke(null)
                    }
                }
            } catch (e: Exception) {
                elseCallBack.invoke(e)
            }
        }
    }
}
