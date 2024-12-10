package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.Account.PREF_ROUTE_BOT_PK
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.MixinApplication
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.session.Session
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.generateConversationId

class RefreshPriceJob(private val assetId: String) : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).requireNetwork().persist(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshPriceJob"
    }

    override fun onRun(): Unit = runBlocking {
        val response = routeService.priceHistory(assetId, "1D")
        if (response.isSuccess && response.data != null) {
            response.data?.let {
                if (it.data.isEmpty()) return@let
                historyPriceDao().insert(it)
            }
        } else if (response.errorCode == ErrorHandler.AUTHENTICATION) {
            val resp = userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
            if (resp.isSuccess) {
                val sessionData = requireNotNull(resp.data)[0]
                MixinApplication.appContext.defaultSharedPreferences.putString(PREF_ROUTE_BOT_PK, sessionData.publicKey)
                participantSessionDao().insertSuspend(ParticipantSession(generateConversationId(sessionData.userId, Session.getAccountId()!!), sessionData.userId, sessionData.sessionId, publicKey = sessionData.publicKey))
            }
        }
    }
}
