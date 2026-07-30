package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import one.mixin.android.Constants.Account.PREF_GLOBAL_MARKET
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.event.GlobalMarketEvent
import one.mixin.android.event.MarketPageDataSource
import one.mixin.android.event.MarketPageRefreshEvent
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.market.MarketCategory
import timber.log.Timber

internal data class SpotMarketRefreshRequest(
    val source: MarketPageDataSource,
    val category: String,
)

class RefreshMarketPageJob(
    private val duration: String,
) : BaseJob(
        Params(PRIORITY_UI_HIGH)
            .singleInstanceBy(GROUP),
    ) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshMarketPageJob"
        private const val SPOT_MARKET_LIMIT = 500
        private const val CATEGORY_ALL = "all"
        private const val CATEGORY_FAVORITE = "favorite"
        internal val SPOT_MARKET_REFRESH_REQUESTS =
            listOf(
                SpotMarketRefreshRequest(MarketPageDataSource.SPOT_ALL, CATEGORY_ALL),
                SpotMarketRefreshRequest(MarketPageDataSource.SPOT_FAVORITE, CATEGORY_FAVORITE),
                SpotMarketRefreshRequest(MarketPageDataSource.SPOT_FEATURED, MarketCategory.FEATURED.apiValue),
                SpotMarketRefreshRequest(MarketPageDataSource.SPOT_TRENDING, MarketCategory.TRENDING.apiValue),
                SpotMarketRefreshRequest(MarketPageDataSource.SPOT_TOP_GAINER, MarketCategory.TOP_GAINER.apiValue),
                SpotMarketRefreshRequest(MarketPageDataSource.SPOT_TOP_LOSER, MarketCategory.TOP_LOSER.apiValue),
                SpotMarketRefreshRequest(MarketPageDataSource.SPOT_ALL, MarketCategory.STOCK.apiValue),
            )
    }

    override fun onRun(): Unit =
        runBlocking {
            val perpsMarketRepository = perpsMarketRepositoryProvider.get()
            supervisorScope {
                val requests =
                    SPOT_MARKET_REFRESH_REQUESTS
                        .map { request ->
                            async {
                                refresh(request.source) {
                                    assetRepo.fetchMarkets(request.category, duration, SPOT_MARKET_LIMIT) != null
                                }
                            }
                        }.toMutableList()
                requests +=
                    async {
                        refresh(MarketPageDataSource.PERPETUAL_ALL) {
                            perpsMarketRepository.syncAllMarkets() != null
                        }
                    }
                requests +=
                    async {
                        refresh(MarketPageDataSource.PERPETUAL_FAVORITE) {
                            perpsMarketRepository.syncFavoriteMarkets() != null
                        }
                    }
                requests +=
                    async {
                        refresh(MarketPageDataSource.PERPETUAL_FEATURED) {
                            perpsMarketRepository.syncCategory(MarketCategory.FEATURED) != null
                        }
                    }
                requests += async { refresh(MarketPageDataSource.GLOBAL, ::refreshGlobalMarket) }
                val results = requests.awaitAll()
                RxBus.publish(
                    MarketPageRefreshEvent(
                        duration = duration,
                        failedSources =
                            results
                                .filterNot { (_, succeeded) -> succeeded }
                                .mapTo(mutableSetOf()) { (source, _) -> source },
                    ),
                )
            }
        }

    private suspend fun refresh(
        source: MarketPageDataSource,
        block: suspend () -> Boolean,
    ): Pair<MarketPageDataSource, Boolean> =
        try {
            source to block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh market source: $source")
            source to false
        }

    private suspend fun refreshGlobalMarket(): Boolean =
        requestRouteAPI(
            invokeNetwork = { routeService.globalMarket() },
            successBlock = { response ->
                val market = response.data ?: return@requestRouteAPI false
                market.let {
                    MixinApplication.appContext.defaultSharedPreferences.putString(
                        PREF_GLOBAL_MARKET,
                        GsonHelper.customGson.toJson(it),
                    )
                    RxBus.publish(GlobalMarketEvent())
                }
                true
            },
            failureBlock = { true },
            exceptionBlock = { true },
            defaultErrorHandle = {},
            defaultExceptionHandle = {},
            requestSession = {
                userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
            },
        ) ?: false
}
