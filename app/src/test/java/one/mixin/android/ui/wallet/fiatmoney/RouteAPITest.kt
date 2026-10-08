package one.mixin.android.ui.wallet.fiatmoney

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.Account.PREF_ROUTE_BOT_PK
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.MixinApplication
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.ResponseError
import one.mixin.android.api.response.UserSession
import one.mixin.android.extension.defaultSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RouteAPITest {
    @Test
    fun waitsForPublicKeyAndStopsWhenSynchronizationFails() = runBlocking {
        MixinApplication.appContext = ApplicationProvider.getApplicationContext()
        val preferences = MixinApplication.appContext.defaultSharedPreferences
        val events = mutableListOf<String>()
        for (mode in listOf("missing", "cached", "failed", "empty")) {
            preferences.edit().remove(PREF_ROUTE_BOT_PK).commit()
            if (mode == "cached") preferences.edit().putString(PREF_ROUTE_BOT_PK, "key").commit()
            events.clear()
            val result = requestRouteAPI<Unit, Boolean>(
                invokeNetwork = {
                    assertEquals("key", preferences.getString(PREF_ROUTE_BOT_PK, null))
                    events.add("request")
                    MixinResponse<Unit>()
                },
                successBlock = { true },
                failureBlock = { error("Session failure must not invoke the wallet failure handler") },
                defaultErrorHandle = { events.add("error") },
                defaultExceptionHandle = { events.add("exception") },
                endBlock = { events.add("end") },
                requestSession = { ids ->
                    assertEquals(listOf(ROUTE_BOT_USER_ID), ids)
                    events.add("sync")
                    when (mode) {
                        "cached" -> error("Cached public key must be reused")
                        "failed" -> MixinResponse(ResponseError(404, 404, "Not found"))
                        else -> MixinResponse<List<UserSession>>().apply {
                            data = listOf(UserSession(ROUTE_BOT_USER_ID, "session", null, if (mode == "empty") "" else "key"))
                        }
                    }
                },
            )
            when (mode) {
                "missing" -> assertEquals(listOf("sync", "request", "end"), events)
                "cached" -> assertEquals(listOf("request", "end"), events)
                "failed" -> assertEquals(listOf("sync", "error", "end"), events)
                "empty" -> assertEquals(listOf("sync", "exception", "end"), events)
            }
            if (mode == "failed" || mode == "empty") assertNull(result) else assertEquals(true, result)
        }
    }
}
