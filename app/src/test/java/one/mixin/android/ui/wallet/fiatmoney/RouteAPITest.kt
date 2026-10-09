package one.mixin.android.ui.wallet.fiatmoney

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.Account.PREF_ROUTE_BOT_PK
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.MixinApplication
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.ResponseError
import one.mixin.android.api.RouteBotPublicKey
import one.mixin.android.api.response.UserSession
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.util.ErrorHandler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RouteAPITest {
    @After
    fun clearPreferences() {
        ApplicationProvider.getApplicationContext<android.content.Context>().defaultSharedPreferences
            .edit().remove(PREF_ROUTE_BOT_PK).commit()
    }

    private fun session(key: String? = "key", userId: String = ROUTE_BOT_USER_ID) = UserSession(userId, "session", null, key)

    private fun response(vararg sessions: UserSession) = MixinResponse<List<UserSession>>().apply { data = sessions.toList() }

    @Test
    fun synchronizesBothCachesAndDeliversFailures() = runBlocking {
        MixinApplication.appContext = ApplicationProvider.getApplicationContext()
        val preferences = MixinApplication.appContext.defaultSharedPreferences
        for (mode in listOf("missing", "cached", "local", "failed", "empty", "blank", "wrong-user", "null")) {
            preferences.edit().remove(PREF_ROUTE_BOT_PK).commit()
            if (mode == "cached") preferences.edit().putString(PREF_ROUTE_BOT_PK, "key").commit()
            val events = mutableListOf<String>()
            val publicKey = RouteBotPublicKey(
                readPreference = { preferences.getString(PREF_ROUTE_BOT_PK, null) },
                readLocal = { if (mode == "local") "key" else null },
                saveSession = { assertEquals(session(), it); events.add("save") },
                savePreference = { preferences.edit().putString(PREF_ROUTE_BOT_PK, it).commit() },
            )
            val result = requestRouteAPI<Unit, Boolean>(
                invokeNetwork = {
                    assertEquals("key", preferences.getString(PREF_ROUTE_BOT_PK, null))
                    events.add("request")
                    MixinResponse<Unit>()
                },
                successBlock = { true },
                failureBlock = { assertEquals(404, it.errorCode); events.add("failure"); true },
                defaultErrorHandle = { error("Failure was handled") },
                defaultExceptionHandle = { events.add("exception") },
                endBlock = { events.add("end") },
                requestSession = { ids ->
                    assertEquals(listOf(ROUTE_BOT_USER_ID), ids)
                    events.add("sync")
                    when (mode) {
                        "cached", "local" -> error("Cached public key must be reused")
                        "failed" -> MixinResponse(ResponseError(404, 404, "Not found"))
                        "empty" -> response()
                        "blank" -> response(session(" "))
                        "null" -> response(session(null))
                        "wrong-user" -> response(session(userId = "other"))
                        else -> response(session(userId = "other"), session())
                    }
                },
                publicKey = publicKey,
            )
            when (mode) {
                "missing" -> assertEquals(listOf("sync", "save", "request", "end"), events)
                "cached", "local" -> assertEquals(listOf("request", "end"), events)
                "failed" -> assertEquals(listOf("sync", "failure", "end"), events)
                else -> assertEquals(listOf("sync", "exception", "end"), events)
            }
            if (mode in listOf("missing", "cached", "local")) assertEquals(true, result) else assertNull(result)
        }
    }

    @Test
    fun concurrentCallersShareSuccessAndFailureAndCanRetry() = runBlocking {
        for (fail in listOf(false, true)) {
            var calls = 0
            var saves = 0
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val publicKey = RouteBotPublicKey({ null }, { null }, { saves++ }, {})
            val fetch: suspend (List<String>) -> MixinResponse<List<UserSession>> = {
                calls++
                started.complete(Unit)
                release.await()
                if (fail) MixinResponse(ResponseError(500, 500, "failed")) else response(session())
            }
            val first = async { runCatching { publicKey.get(fetch) } }
            started.await()
            val second = async(start = CoroutineStart.UNDISPATCHED) { runCatching { publicKey.get(fetch) } }
            release.complete(Unit)
            assertEquals(fail, first.await().isFailure)
            assertEquals(fail, second.await().isFailure)
            assertEquals(1, calls)
            assertEquals(if (fail) 0 else 1, saves)
            assertEquals("key", publicKey.get({ response(session()) }))
        }
    }

    @Test
    fun authenticationRetryUsesValidatedSessionAndEndsOnce() = runBlocking {
        for (mode in listOf("success", "failed", "empty")) {
            var key = "old"
            var saved: UserSession? = null
            var requests = 0
            var failures = 0
            var exceptions = 0
            var ends = 0
            val publicKey = RouteBotPublicKey({ key }, { error("Forced refresh must skip database") }, { saved = it }, { key = it })
            requestRouteAPI<Unit, Unit>(
                invokeNetwork = {
                    requests++
                    if (requests == 1) MixinResponse(ResponseError(401, ErrorHandler.AUTHENTICATION, "Unauthorized"))
                    else { assertEquals("key", key); MixinResponse() }
                },
                failureBlock = { failures++; true },
                exceptionBlock = { exceptions++; true },
                endBlock = { ends++ },
                requestSession = {
                    when (mode) {
                        "failed" -> MixinResponse(ResponseError(500, 500, "failed"))
                        "empty" -> response()
                        else -> response(session(userId = "other"), session())
                    }
                },
                publicKey = publicKey,
            )
            assertEquals(1, ends)
            assertEquals(if (mode == "success") 2 else 1, requests)
            assertEquals(if (mode == "failed") 1 else 0, failures)
            assertEquals(if (mode == "empty") 1 else 0, exceptions)
            assertEquals(if (mode == "success") session() else null, saved)
        }
    }

    @Test
    fun cancellationEndsOnceAndAllowsAnotherSynchronization() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val publicKey = RouteBotPublicKey({ null }, { null }, {}, {})
        var ends = 0
        val request = launch {
            requestRouteAPI<Unit, Unit>(
                invokeNetwork = { error("Cancelled synchronization must not send a request") },
                exceptionBlock = { error("Cancellation must propagate") },
                endBlock = { ends++ },
                requestSession = { started.complete(Unit); awaitCancellation() },
                publicKey = publicKey,
            )
        }
        started.await()
        request.cancelAndJoin()
        assertEquals(1, ends)
        assertEquals("key", publicKey.get({ response(session()) }))
    }

    @Test
    fun throwingCallbacksDoNotRepeatEndBlock() = runBlocking {
        for (callback in listOf("error", "end")) {
            var ends = 0
            val result = runCatching {
                requestRouteAPI<Unit, Unit>(
                    invokeNetwork = { MixinResponse(ResponseError(500, 500, "failed")) },
                    defaultErrorHandle = { if (callback == "error") error("handler failed") },
                    endBlock = { ends++; if (callback == "end") error("end failed") },
                    requestSession = { error("Cached") },
                    publicKey = RouteBotPublicKey({ "key" }, { null }, {}, {}),
                )
            }
            assertTrue(result.isFailure)
            assertEquals(1, ends)
        }
    }
}
