package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI

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
            successBlock = { response ->
                if (response.isSuccess) {
                    globalMarketDao.insert(response.data!!)
                }
            },
            requestSession = { userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID)) },
        )

    }
}
