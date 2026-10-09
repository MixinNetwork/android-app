package one.mixin.android.api

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.Account.PREF_ROUTE_BOT_PK
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.MixinApplication
import one.mixin.android.api.response.UserSession
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.session.Session
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.generateConversationId

class RouteBotPublicKey(
    private val readPreference: () -> String?,
    private val readLocal: suspend () -> String?,
    private val saveSession: suspend (UserSession) -> Unit,
    private val savePreference: (String) -> Unit,
) {
    private val mutex = Mutex()
    private var pending: CompletableDeferred<String>? = null

    suspend fun get(
        requestSession: suspend (List<String>) -> MixinResponse<List<UserSession>>,
        force: Boolean = false,
    ): String {
        var owner = false
        val result = mutex.withLock {
            pending ?: CompletableDeferred<String>().also {
                pending = it
                owner = true
            }
        }
        if (!owner) return result.await()
        try {
            val key = withContext(Dispatchers.IO) {
                val cached = if (force) null else readPreference()?.takeIf { it.isNotBlank() }
                    ?: readLocal()?.takeIf { it.isNotBlank() }
                cached ?: run {
                    val response = requestSession(listOf(ROUTE_BOT_USER_ID))
                    if (!response.isSuccess) {
                        throw MixinResponseException(response.errorCode, response.errorDescription)
                    }
                    val session = response.data?.firstOrNull {
                        it.userId == ROUTE_BOT_USER_ID && !it.publicKey.isNullOrBlank()
                    }
                    if (session == null) throw IOException("Route bot public key is missing")
                    saveSession(session)
                    requireNotNull(session.publicKey)
                }
            }
            savePreference(key)
            result.complete(key)
            return key
        } catch (t: Throwable) {
            result.completeExceptionally(t)
            throw t
        } finally {
            withContext(NonCancellable) { mutex.withLock { pending = null } }
        }
    }

    companion object {
        val shared = RouteBotPublicKey(
            readPreference = { MixinApplication.appContext.defaultSharedPreferences.getString(PREF_ROUTE_BOT_PK, null) },
            readLocal = {
                val account = requireNotNull(Session.getAccount())
                MixinDatabase.getDatabase(MixinApplication.appContext, account.identityNumber)
                    .participantSessionDao().findBotPublicKey(generateConversationId(ROUTE_BOT_USER_ID, account.userId), ROUTE_BOT_USER_ID)
            },
            saveSession = { session ->
                val account = requireNotNull(Session.getAccount())
                MixinDatabase.getDatabase(MixinApplication.appContext, account.identityNumber)
                    .participantSessionDao().insertSuspend(
                        ParticipantSession(generateConversationId(session.userId, account.userId), session.userId, session.sessionId, publicKey = session.publicKey),
                    )
            },
            savePreference = { MixinApplication.appContext.defaultSharedPreferences.putString(PREF_ROUTE_BOT_PK, it) },
        )
    }
}
