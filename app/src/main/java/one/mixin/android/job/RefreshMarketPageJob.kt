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
import one.mixin.android.api.response.perps.PerpsMarketCategory
import one.mixin.android.event.GlobalMarketEvent
import one.mixin.android.event.MarketPageDataSource
import one.mixin.android.event.MarketPageRefreshEvent
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.market.MarketCategory
import timber.log.Timber

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
    }

    override fun onRun(): Unit =
        runBlocking {
            val perpsMarketRepository = perpsMarketRepositoryProvider.get()
            supervisorScope {
                val results =
                    listOf(
                        async {
                            refresh(MarketPageDataSource.SPOT_ALL) {
                                assetRepo.fetchMarkets(CATEGORY_ALL, duration, SPOT_MARKET_LIMIT) != null
                            }
                        },
                        async {
                            refresh(MarketPageDataSource.SPOT_FAVORITE) {
                                assetRepo.fetchMarkets(CATEGORY_FAVORITE, duration, SPOT_MARKET_LIMIT) != null
                            }
                        },
                        async {
                            refresh(MarketPageDataSource.SPOT_FEATURED) {
                                assetRepo.fetchMarkets(MarketCategory.FEATURED.apiValue, duration, SPOT_MARKET_LIMIT) != null
                            }
                        },
                        async {
                            refresh(MarketPageDataSource.SPOT_TRENDING) {
                                assetRepo.fetchMarkets(MarketCategory.TRENDING.apiValue, duration, SPOT_MARKET_LIMIT) != null
                            }
                        },
                        async {
                            refresh(MarketPageDataSource.SPOT_TOP_GAINER) {
                                assetRepo.fetchMarkets(MarketCategory.TOP_GAINER.apiValue, duration, SPOT_MARKET_LIMIT) != null
                            }
                        },
                        async {
                            refresh(MarketPageDataSource.SPOT_TOP_LOSER) {
                                assetRepo.fetchMarkets(MarketCategory.TOP_LOSER.apiValue, duration, SPOT_MARKET_LIMIT) != null
                            }
                        },
                        async {
                            refresh(MarketPageDataSource.SPOT_STOCK) {
                                assetRepo.fetchMarkets(MarketCategory.STOCK.apiValue, duration, SPOT_MARKET_LIMIT) != null
                            }
                        },
                        async {
                            refresh(MarketPageDataSource.PERPETUAL_ALL) {
                                perpsMarketRepository.syncAllMarkets() != null
                            }
                        },
                        async {
                            refresh(MarketPageDataSource.PERPETUAL_FAVORITE) {
                                perpsMarketRepository.syncFavoriteMarkets() != null
                            }
                        },
                        async {
                            refresh(MarketPageDataSource.PERPETUAL_FEATURED) {
                                perpsMarketRepository.syncCategory(PerpsMarketCategory.FEATURED) != null
                            }
                        },
                        async {
                            refresh(MarketPageDataSource.PERPETUAL_TRENDING) {
                                perpsMarketRepository.syncCategory(PerpsMarketCategory.TRENDING) != null
                            }
                        },
                        async {
                            refresh(MarketPageDataSource.GLOBAL, ::refreshGlobalMarket)
                        },
                    ).awaitAll()
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
