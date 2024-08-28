package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.vo.market.MarketExtra

class UpdateFavoriteJob(private val coinId: String, private val isFavored: Boolean?) : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "UpdateFavoriteJob"
    }

    override fun onAdded() {
        super.onAdded()
        marketExtraDao.update(coinId, isFavored != true)
    }

    override fun onRun(): Unit = runBlocking {
        if (isFavored == true) {
            requestRouteAPI(
                invokeNetwork = { routeService.unfavorite(coinId) },
                successBlock = { response ->
                    if (response.isSuccess && response.data != null) {
                        response.data?.let { market ->
                            marketExtraDao.insertList(market.assetIds.map { assetId ->
                                MarketExtra(
                                    coinId = market.coinId,
                                    assetId = assetId,
                                    isFavored = false
                                )
                            })
                        }
                    }
                },
                requestSession = {
                    userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
                }
            )
        } else {
            requestRouteAPI(
                invokeNetwork = { routeService.favorite(coinId) },
                successBlock = { response ->
                    if (response.isSuccess && response.data != null) {
                        response.data?.let { market ->
                            marketExtraDao.insertList(market.assetIds.map { assetId ->
                                MarketExtra(
                                    coinId = market.coinId,
                                    assetId = assetId,
                                    isFavored = true
                                )
                            })
                        }
                    }
                },
                requestSession = {
                    userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
                }
            )
        }
    }
}
