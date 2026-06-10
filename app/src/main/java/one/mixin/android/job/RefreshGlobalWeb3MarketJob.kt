package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.Account.PREF_GLOBAL_MARKET
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.event.GlobalMarketEvent
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.util.GsonHelper

class RefreshGlobalWeb3MarketJob() : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshMarketJob"
    }

    override fun onRun(): Unit = runBlocking{
        requestRouteAPI(
            invokeNetwork = { routeService.globalMarket() },
            defaultErrorHandle = {},
            defaultExceptionHandle = {},
            successBlock = { response ->
                if (response.isSuccess) {
                    MixinApplication.appContext.defaultSharedPreferences.putString(PREF_GLOBAL_MARKET, GsonHelper.customGson.toJson(response.data!!))
                    RxBus.publish(GlobalMarketEvent())
                }
            },
            requestSession = { userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID)) },
        )

    }
}
