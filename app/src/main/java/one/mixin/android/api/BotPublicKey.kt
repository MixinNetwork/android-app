package one.mixin.android.api

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.Account.PREF_CASH_BOT_PK
import one.mixin.android.Constants.Account.PREF_EARN_BOT_PK
import one.mixin.android.Constants.Account.PREF_REFERRAL_BOT_PK
import one.mixin.android.Constants.Account.PREF_ROUTE_BOT_PK
import one.mixin.android.Constants.MIXIN_CASH_USER_ID
import one.mixin.android.Constants.MIXIN_EARN_USER_ID
import one.mixin.android.Constants.RouteConfig.REFERRAL_BOT_USER_ID
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.MixinApplication
import one.mixin.android.api.response.UserSession
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.session.Session
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.generateConversationId

class BotPublicKey(
    private val readPreference: () -> String?,
    private val readLocal: suspend () -> String?,
    private val saveSession: suspend (UserSession) -> Unit,
    private val savePreference: (String) -> Unit,
    private val botId: String = ROUTE_BOT_USER_ID,
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
        val outcome = runCatching {
            withContext(Dispatchers.IO) {
                if (!force) {
                    readPreference()?.takeIf { it.isNotBlank() }?.let { return@withContext it }
                }
                val cached = if (force) null else readLocal()?.takeIf { it.isNotBlank() }
                (cached ?: run {
                    val response = requestSession(listOf(botId))
                    if (!response.isSuccess) {
                        throw MixinResponseException(response.errorCode, response.errorDescription)
                    }
                    val session = response.data?.firstOrNull {
                        it.userId == botId && !it.publicKey.isNullOrBlank()
                    }
                    if (session == null) throw IOException("Bot public key is missing")
                    saveSession(session)
                    requireNotNull(session.publicKey)
                }).also(savePreference)
            }
        }
        withContext(NonCancellable) {
            mutex.withLock {
                pending = null
                outcome.fold(result::complete, result::completeExceptionally)
            }
        }
        return outcome.getOrThrow()
    }

    companion object {
        private val bots = mapOf(
            ROUTE_BOT_USER_ID to create(ROUTE_BOT_USER_ID, PREF_ROUTE_BOT_PK),
            REFERRAL_BOT_USER_ID to create(REFERRAL_BOT_USER_ID, PREF_REFERRAL_BOT_PK),
            MIXIN_CASH_USER_ID to create(MIXIN_CASH_USER_ID, PREF_CASH_BOT_PK),
            MIXIN_EARN_USER_ID to create(MIXIN_EARN_USER_ID, PREF_EARN_BOT_PK),
        )
        val route = requireNotNull(bots[ROUTE_BOT_USER_ID])

        fun forBot(botId: String): BotPublicKey? = bots[botId]

        private fun create(botId: String, preferenceKey: String) = BotPublicKey(
            readPreference = { MixinApplication.appContext.defaultSharedPreferences.getString(preferenceKey, null) },
            readLocal = {
                val account = requireNotNull(Session.getAccount())
                MixinDatabase.getDatabase(MixinApplication.appContext, account.identityNumber)
                    .participantSessionDao().findBotPublicKey(generateConversationId(botId, account.userId), botId)
            },
            saveSession = { session ->
                val account = requireNotNull(Session.getAccount())
                MixinDatabase.getDatabase(MixinApplication.appContext, account.identityNumber)
                    .participantSessionDao().insertSuspend(
                        ParticipantSession(generateConversationId(session.userId, account.userId), session.userId, session.sessionId, publicKey = session.publicKey),
                    )
            },
            savePreference = { MixinApplication.appContext.defaultSharedPreferences.putString(preferenceKey, it) },
            botId = botId,
        )
    }
}
