package one.mixin.android.ui.landing.viewmodel

import androidx.collection.ArrayMap
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.SessionSecretRequest
import one.mixin.android.api.response.UserSession
import one.mixin.android.api.service.AccountService
import one.mixin.android.api.service.SignalKeyService
import one.mixin.android.api.service.UserService
import one.mixin.android.crypto.db.SignalDatabase
import one.mixin.android.crypto.vo.SenderKey
import one.mixin.android.crypto.vo.Session
import one.mixin.android.db.DatabaseProvider
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.getDeviceId
import one.mixin.android.job.RefreshOneTimePreKeysJob
import one.mixin.android.vo.ParticipantSession
import javax.inject.Inject
import kotlin.collections.set
import kotlin.sequences.forEach

@HiltViewModel
class LoadingViewModel
@Inject
internal constructor(
    private val databaseProvider: DatabaseProvider,
    private val signalDatabase: SignalDatabase,
    private val accountService: AccountService,
    private val signalKeyService: SignalKeyService,
    private val userService: UserService,
) : ViewModel() {

    suspend fun pushAsyncSignalKeys(): MixinResponse<Void> =
        withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            val signalKeys = RefreshOneTimePreKeysJob.Companion.generateKeys()
            val response = signalKeyService.pushSignalKeys(signalKeys).await()
            val time = System.currentTimeMillis() - start
            if (time < 2000) {
                delay(time)
            }
            return@withContext response
        }

    suspend fun updateSignalSession() {
        withContext(Dispatchers.IO) {
            val sessions = signalDatabase.sessionDao().syncGetSessionAddress()
            val userIds = sessions.map { it.address }
            val response = userService.fetchSessionsSuspend(userIds)
            val sessionMap = ArrayMap<String, Int>()
            val userSessionMap = ArrayMap<String, UserSession>()
            if (response.isSuccess) {
                response.data?.asSequence()?.forEach { item ->
                    if (item.platform == "Android" || item.platform == "iOS") {
                        val deviceId = item.sessionId.getDeviceId()
                        sessionMap[item.userId] = deviceId
                        userSessionMap[item.userId] = item
                    }
                }
            }
            if (sessionMap.isEmpty()) {
                return@withContext
            }
            val newSession = mutableListOf<Session>()
            for (s in sessions) {
                sessionMap[s.address]?.let { d ->
                    newSession.add(Session(s.address, d, s.record, s.timestamp))
                }
            }
            signalDatabase.sessionDao().insertList(newSession)
            val senderKeys = signalDatabase.senderKeyDao().syncGetSenderKeys()
            senderKeys.forEach { key ->
                if (!key.senderId.endsWith(":1")) {
                    return@forEach
                }
                val userId = key.senderId.substring(0, key.senderId.length - 2)
                sessionMap[userId]?.let { d ->
                    signalDatabase.senderKeyDao().insert(SenderKey(key.groupId, "$userId:$d", key.record))
                }
            }
            databaseProvider.initAllDatabases()
            val participants = databaseProvider.getMixinDatabase().participantDao().getAllParticipants()
            val newParticipantSession = mutableListOf<ParticipantSession>()
            participants.forEach { p ->
                userSessionMap[p.userId]?.let { userSession ->
                    val ps = ParticipantSession(
                        p.conversationId,
                        p.userId,
                        sessionId = userSession.sessionId,
                        publicKey = userSession.publicKey
                    )
                    newParticipantSession.add(ps)
                }
            }
            databaseProvider.getMixinDatabase().participantSessionDao().insertListSuspend(newParticipantSession)
        }
    }

    suspend fun modifySessionSecret(request: SessionSecretRequest) =
        withContext(Dispatchers.IO) {
            accountService.modifySessionSecret(request)
        }
}