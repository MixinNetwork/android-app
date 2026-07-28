package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import one.mixin.android.Constants.Account.PREF_GLOBAL_MARKET
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.api.response.perps.PerpsMarketCategory
import one.mixin.android.event.GlobalMarketEvent
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.market.MarketCategory

class RefreshMarketPageJob(
    private val duration: String,
) : BaseJob(
        Params(PRIORITY_UI_HIGH)
            .singleInstanceBy(GROUP)
            .requireNetwork(),
    ) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshMarketPageJob"
        private const val SPOT_MARKET_LIMIT = 500
        private const val CATEGORY_ALL = "all"
        private const val CATEGORY_FAVORITE = "favorite"
    }

    override fun onRun(): Unit =
        runBlocking {
            val perpsMarketRepository = perpsMarketRepositoryProvider.get()
            supervisorScope {
                listOf(
                    async { assetRepo.fetchMarkets(CATEGORY_ALL, duration, SPOT_MARKET_LIMIT) },
                    async { assetRepo.fetchMarkets(CATEGORY_FAVORITE, duration, SPOT_MARKET_LIMIT) },
                    async { assetRepo.fetchMarkets(MarketCategory.TRENDING.apiValue, duration, SPOT_MARKET_LIMIT) },
                    async { assetRepo.fetchMarkets(MarketCategory.TOP_GAINER.apiValue, duration, SPOT_MARKET_LIMIT) },
                    async { assetRepo.fetchMarkets(MarketCategory.TOP_LOSER.apiValue, duration, SPOT_MARKET_LIMIT) },
                    async { assetRepo.fetchMarkets(MarketCategory.STOCK.apiValue, duration, SPOT_MARKET_LIMIT) },
                    async { perpsMarketRepository.syncAllMarkets() },
                    async { perpsMarketRepository.syncFavoriteMarkets() },
                    async { perpsMarketRepository.syncCategory(PerpsMarketCategory.TRENDING) },
                    async { perpsMarketRepository.syncCategory(PerpsMarketCategory.TOP_GAINER) },
                    async { perpsMarketRepository.syncCategory(PerpsMarketCategory.TOP_LOSER) },
                    async { refreshGlobalMarket() },
                ).awaitAll()
            }
        }

    private suspend fun refreshGlobalMarket() {
        requestRouteAPI(
            invokeNetwork = { routeService.globalMarket() },
            successBlock = { response ->
                response.data?.let { market ->
                    MixinApplication.appContext.defaultSharedPreferences.putString(
                        PREF_GLOBAL_MARKET,
                        GsonHelper.customGson.toJson(market),
                    )
                    RxBus.publish(GlobalMarketEvent())
                }
            },
            failureBlock = { true },
            exceptionBlock = { true },
            defaultErrorHandle = {},
            defaultExceptionHandle = {},
            requestSession = {
                userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
            },
        )
    }

}
