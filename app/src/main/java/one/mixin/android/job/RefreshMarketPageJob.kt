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
import one.mixin.android.event.ALL_MARKET_PAGE_DATA_SOURCES
import one.mixin.android.event.GlobalMarketEvent
import one.mixin.android.event.MarketPageDataSource
import one.mixin.android.event.MarketPageRefreshEvent
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putString
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.market.MarketCategory
import timber.log.Timber

private const val SPOT_MARKET_LIMIT = 500
private const val TOP_GAINER_LOSER_MARKET_LIMIT = 100

internal data class SpotMarketRefreshRequest(
    val source: MarketPageDataSource,
    val category: String,
    val limit: Int = SPOT_MARKET_LIMIT,
)

internal data class MarketPageRefreshPlan(
    val spotRequests: List<SpotMarketRefreshRequest>,
    val refreshesPerpetualAll: Boolean,
    val refreshesPerpetualFavorite: Boolean,
    val refreshesPerpetualFeatured: Boolean,
    val refreshesGlobal: Boolean,
)

internal fun marketPageRefreshPlan(sources: Set<MarketPageDataSource>): MarketPageRefreshPlan =
    MarketPageRefreshPlan(
        spotRequests = RefreshMarketPageJob.SPOT_MARKET_REFRESH_REQUESTS.filter { it.source in sources },
        refreshesPerpetualAll = MarketPageDataSource.PERPETUAL_ALL in sources,
        refreshesPerpetualFavorite = MarketPageDataSource.PERPETUAL_FAVORITE in sources,
        refreshesPerpetualFeatured = MarketPageDataSource.PERPETUAL_FEATURED in sources,
        refreshesGlobal = MarketPageDataSource.GLOBAL in sources,
    )

private fun refreshMarketPageSingleInstanceId(
    duration: String,
    sources: Set<MarketPageDataSource>,
): String =
    buildString {
        append(RefreshMarketPageJob.GROUP)
        append(':')
        append(duration)
        sources.sortedBy { it.ordinal }.forEach { source ->
            append(':')
            append(source.name)
        }
    }

class RefreshMarketPageJob(
    private val duration: String,
    sources: Set<MarketPageDataSource> = ALL_MARKET_PAGE_DATA_SOURCES,
) : BaseJob(
        Params(PRIORITY_UI_HIGH)
            .groupBy(GROUP)
            .singleInstanceBy(refreshMarketPageSingleInstanceId(duration, sources)),
    ) {
    private val sources = sources.toSet()

    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshMarketPageJob"
        private const val CATEGORY_ALL = "all"
        private const val CATEGORY_FAVORITE = "favorite"
        internal val SPOT_MARKET_REFRESH_REQUESTS =
            listOf(
                SpotMarketRefreshRequest(MarketPageDataSource.SPOT_ALL, CATEGORY_ALL),
                SpotMarketRefreshRequest(MarketPageDataSource.SPOT_FAVORITE, CATEGORY_FAVORITE),
                SpotMarketRefreshRequest(MarketPageDataSource.SPOT_FEATURED, MarketCategory.FEATURED.apiValue),
                SpotMarketRefreshRequest(MarketPageDataSource.SPOT_TRENDING, MarketCategory.TRENDING.apiValue),
                SpotMarketRefreshRequest(
                    MarketPageDataSource.SPOT_TOP_GAINER,
                    MarketCategory.TOP_GAINER.apiValue,
                    TOP_GAINER_LOSER_MARKET_LIMIT,
                ),
                SpotMarketRefreshRequest(
                    MarketPageDataSource.SPOT_TOP_LOSER,
                    MarketCategory.TOP_LOSER.apiValue,
                    TOP_GAINER_LOSER_MARKET_LIMIT,
                ),
                SpotMarketRefreshRequest(MarketPageDataSource.SPOT_ALL, MarketCategory.STOCK.apiValue),
            )
    }

    init {
        require(sources.isNotEmpty())
    }

    override fun onRun(): Unit =
        runBlocking {
            val perpsMarketRepository = perpsMarketRepositoryProvider.get()
            supervisorScope {
                val plan = marketPageRefreshPlan(sources)
                val requests =
                    plan.spotRequests
                        .map { request ->
                            async {
                                refresh(request.source) {
                                    assetRepo.fetchMarkets(request.category, duration, request.limit) != null
                                }
                            }
                        }.toMutableList()
                if (plan.refreshesPerpetualAll) {
                    requests +=
                        async {
                            refresh(MarketPageDataSource.PERPETUAL_ALL) {
                                perpsMarketRepository.syncAllMarkets() != null
                            }
                        }
                }
                if (plan.refreshesPerpetualFavorite) {
                    requests +=
                        async {
                            refresh(MarketPageDataSource.PERPETUAL_FAVORITE) {
                                perpsMarketRepository.syncFavoriteMarkets() != null
                            }
                        }
                }
                if (plan.refreshesPerpetualFeatured) {
                    requests +=
                        async {
                            refresh(MarketPageDataSource.PERPETUAL_FEATURED) {
                                perpsMarketRepository.syncCategory(MarketCategory.FEATURED) != null
                            }
                        }
                }
                if (plan.refreshesGlobal) {
                    requests += async { refresh(MarketPageDataSource.GLOBAL, ::refreshGlobalMarket) }
                }
                val results = requests.awaitAll()
                RxBus.publish(
                    MarketPageRefreshEvent(
                        duration = duration,
                        refreshedSources = sources,
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
