package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.api.request.RouteInstrumentRequest
import one.mixin.android.session.Session
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.vo.market.MarketExtra
import timber.log.Timber

class RefreshMarketsJob(val category: String = "all") : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshMarketsJob"
    }

    override fun onRun(): Unit = runBlocking {
        requestRouteAPI(
            invokeNetwork = {
                routeService.markets(category = category)
            },
            successBlock = { response ->
                val list = response.data!!
                if (category == "favorite") {
                    val marketExtraList: List<MarketExtra> = list.flatMap { market ->
                        market.assetIds.map { assetId ->
                            MarketExtra(
                                coinId = market.coinId,
                                assetId = assetId,
                                isFavored = true
                            )
                        }
                    }
                    marketExtraDao.insertList(marketExtraList)
                }
                try {
                    marketDao.insertList(list)
                } catch (e: Exception) {
                    Timber.e("error ${e.message}")
                }
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
            }
        )
    }
}
