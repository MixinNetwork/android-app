package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.db.runInTransaction
import one.mixin.android.extension.nowInUtc
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.vo.market.MarketCoin
import one.mixin.android.vo.market.MarketFavored

class RefreshAlertsJob() : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .groupBy(GROUP).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshAlertsJob"
    }

    override fun onRun(): Unit = runBlocking {
        requestRouteAPI(
            invokeNetwork = {
                routeService.alerts()
            },
            successBlock = { response ->
                val list = response.data!!
                runInTransaction {
                    alertDao.deleteAll()
                    alertDao.insertList(list)
                }
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
            }
        )
    }
}
