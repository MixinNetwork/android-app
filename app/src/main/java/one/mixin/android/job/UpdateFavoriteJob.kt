package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.toast
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.vo.market.MarketFavored

class UpdateFavoriteJob(private val symbol: String, private val coinId: String, private val isFavored: Boolean?) : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "UpdateFavoriteJob"
    }

    override fun onRun(): Unit = runBlocking {
        val now = nowInUtc()
        if (isFavored == true) {
            requestRouteAPI(
                invokeNetwork = { routeService.unfavorite(coinId) },
                defaultErrorHandle = {},
                defaultExceptionHandle = {},
                successBlock = { _ ->
                    marketFavoredDao.insert(
                        MarketFavored(
                            coinId = coinId,
                            isFavored = false,
                            now
                        )
                    )
                },
                requestSession = {
                    userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
                }
            )
        } else {
            requestRouteAPI(
                invokeNetwork = { routeService.favorite(coinId) },
                defaultErrorHandle = {},
                defaultExceptionHandle = {},
                successBlock = { _ ->
                    marketFavoredDao.insert(
                        MarketFavored(
                            coinId = coinId,
                            isFavored = true,
                            now
                        )
                    )
                    withContext(Dispatchers.Main) {
                        toast(MixinApplication.appContext.getString(R.string.watchlist_add_desc, symbol))
                    }
                },
                requestSession = {
                    userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
                }
            )
        }
    }
}
