package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.db.runInTransaction
import one.mixin.android.extension.nowInUtc
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.vo.market.MarketCoin

class RefreshAlertsJob : BaseJob(
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
            defaultErrorHandle = {},
            defaultExceptionHandle = {},
            successBlock = { response ->
                val list = response.data!!
                list.map{it.coinId}.distinct().mapNotNull { coinId ->
                    val m = marketDao.findMarketById(coinId)
                    if (m != null) null
                    else coinId
                }.let { ids ->
                    if (ids.isNotEmpty()) {
                        refreshMark(ids)
                    }
                }
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

    private suspend fun refreshMark(ids: List<String>) {
        requestRouteAPI(
            invokeNetwork = {
                routeService.fetchMarket(ids)
            },
            defaultErrorHandle = {},
            defaultExceptionHandle = {},
            successBlock = { response ->
                val list = response.data!!
                val now = nowInUtc()
                val coins = list.flatMap { market ->
                    market.assetIds?.map { assetId ->
                        MarketCoin(
                            coinId = market.coinId,
                            assetId = assetId,
                            createdAt = now
                        )
                    } ?: emptyList()
                }
                marketCoinDao.insertList(coins)
                marketDao.insertList(list)
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
            }
        )
    }
}
