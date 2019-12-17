package one.mixin.android.ui.landing

import androidx.collection.ArrayMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlin.math.abs
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
import one.mixin.android.crypto.vo.Session
import one.mixin.android.extension.getDeviceId
import one.mixin.android.job.RefreshOneTimePreKeysJob
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.vo.ParticipantSession

class LoadingViewModel @Inject internal
constructor(
    private val signalKeyService: SignalKeyService,
    private val accountService: AccountService,
    private val userService: UserService,
    private val conversationRepo: ConversationRepository
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
            val userIds = sessions.map { it.address }
            val response = userService.fetchSessionsSuspend(userIds)
            val sessionMap = ArrayMap<String, Int>()
            val userSessionMap = ArrayMap<String, String>()
            if (response.isSuccess) {
                response.data?.asSequence()?.forEach { item ->
                    if (item.platform == "Android" || item.platform == "iOS") {
                        val deviceId = item.sessionId.getDeviceId()
                        sessionMap[item.userId] = deviceId
                        userSessionMap[item.userId] = item.sessionId
                    }
                }
            }
            if (sessionMap.isEmpty) {
                return@withContext
            }
            val newSession = mutableListOf<Session>()
            for (s in sessions) {
                sessionMap[s.address]?.let { d ->
                    newSession.add(Session(s.address, d, s.record, s.timestamp))
                }
            }
            sessionDao.insertList(newSession)
            val senderKeys = senderKeyDao.syncGetSenderKeys()
            senderKeys.forEach { key ->
                if (!key.senderId.endsWith(":1")) {
                    return@forEach
                }
                val userId = key.senderId.substring(0, key.senderId.length - 2)
                sessionMap[userId]?.let { d ->
                    senderKeyDao.insert(SenderKey(key.groupId, "$userId:$d", key.record))
                }
            }

            val participants = conversationRepo.getAllParticipants()
            val newParticipantSession = mutableListOf<ParticipantSession>()
            participants.forEach { p ->
                userSessionMap[p.userId]?.let {
                    val ps = ParticipantSession(p.conversationId, p.userId, it)
                    newParticipantSession.add(ps)
                }
            }
            conversationRepo.insertParticipantSession(newParticipantSession)
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
