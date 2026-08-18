package one.mixin.android.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import one.mixin.android.Constants
import one.mixin.android.api.response.perps.PerpsFavorite
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.api.response.perps.withDefaults
import one.mixin.android.api.service.RouteService
import one.mixin.android.api.service.UserService
import one.mixin.android.db.PerpsDatabase
import one.mixin.android.db.perps.PerpsFavoriteDao
import one.mixin.android.db.perps.PerpsMarketCategoryDao
import one.mixin.android.db.perps.PerpsMarketDao
import one.mixin.android.extension.nowInUtc
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.vo.market.MarketCategory
import javax.inject.Inject

class PerpsMarketRepository
    @Inject
    constructor(
        private val routeService: RouteService,
        private val userService: UserService,
        private val database: PerpsDatabase,
        private val marketDao: PerpsMarketDao,
        private val favoriteDao: PerpsFavoriteDao,
        private val categoryDao: PerpsMarketCategoryDao,
    ) {
        fun observeAllMarkets(): Flow<List<PerpsMarket>> = marketDao.observeAllMarkets()

        fun observeFavoriteMarkets(): Flow<List<PerpsMarket>> = favoriteDao.observeFavoriteMarkets()

        fun observeFavoriteMarketIds(): Flow<Set<String>> =
            favoriteDao.observeFavoriteMarketIds().map { marketIds -> marketIds.toSet() }

        fun observeMarketsByCategory(category: MarketCategory): Flow<List<PerpsMarket>> =
            categoryDao.observeMarketsByCategory(category.value)

        suspend fun getAllMarkets(): List<PerpsMarket> = marketDao.getAllMarkets()

        suspend fun getMarket(marketId: String): PerpsMarket? = marketDao.getMarket(marketId)

        suspend fun syncAllMarkets(): List<PerpsMarket>? {
            val markets = fetchMarkets() ?: return null
            marketDao.upsertList(markets)
            return markets
        }

        suspend fun syncFavoriteMarkets(): List<PerpsMarket>? {
            val markets = fetchMarkets(CATEGORY_FAVORITE) ?: return null
            return database.withTransaction {
                val mergedMarkets = marketDao.upsertPreservingTradeVolumeScores(markets)
                favoriteDao.replaceAll(
                    marketIds = mergedMarkets.map(PerpsMarket::marketId),
                    createdAt = nowInUtc(),
                )
                mergedMarkets
            }
        }

        suspend fun syncCategory(category: MarketCategory): List<PerpsMarket>? {
            val markets = fetchMarkets(category.apiValue) ?: return null
            return database.withTransaction {
                val mergedMarkets =
                    if (category == MarketCategory.TRENDING) {
                        marketDao.upsertList(markets)
                        markets
                    } else {
                        marketDao.upsertPreservingTradeVolumeScores(markets)
                    }
                categoryDao.replaceCategory(
                    category = category.value,
                    marketIds = mergedMarkets.map(PerpsMarket::marketId),
                )
                mergedMarkets
            }
        }

        suspend fun refreshMarket(marketId: String): PerpsMarket? =
            requestRouteAPI(
                invokeNetwork = { routeService.getPerpsMarket(marketId) },
                successBlock = { response ->
                    response.data?.withDefaults()?.let { market ->
                        marketDao.upsertPreservingTradeVolumeScores(listOf(market)).single()
                    }
                },
                failureBlock = { true },
                exceptionBlock = { true },
                defaultErrorHandle = {},
                defaultExceptionHandle = {},
                requestSession = {
                    userService.fetchSessionsSuspend(listOf(Constants.RouteConfig.ROUTE_BOT_USER_ID))
                },
            )

        suspend fun getOrRefreshMarket(marketId: String): PerpsMarket? =
            marketDao.getMarket(marketId) ?: refreshMarket(marketId)

        suspend fun updateFavorite(
            marketId: String,
            isFavored: Boolean,
        ): Boolean =
            requestRouteAPI(
                invokeNetwork = {
                    if (isFavored) {
                        routeService.unfavoritePerpsMarket(marketId)
                    } else {
                        routeService.favoritePerpsMarket(marketId)
                    }
                },
                successBlock = {
                    favoriteDao.upsertSuspend(
                        PerpsFavorite(
                            marketId = marketId,
                            isFavored = !isFavored,
                            createdAt = nowInUtc(),
                        ),
                    )
                    true
                },
                failureBlock = { true },
                exceptionBlock = { true },
                defaultErrorHandle = {},
                defaultExceptionHandle = {},
                requestSession = {
                    userService.fetchSessionsSuspend(listOf(Constants.RouteConfig.ROUTE_BOT_USER_ID))
                },
            ) ?: false

        suspend fun addFavoriteMarkets(marketIds: Set<String>): Set<String> {
            val favoriteMarketIds = favoriteDao.favoriteMarketIds().toSet()
            val addedMarketIds = marketIds - favoriteMarketIds
            if (addedMarketIds.isEmpty()) return emptySet()
            return requestRouteAPI(
                invokeNetwork = {
                    routeService.updatePerpsMarketFavorites(addedMarketIds.toList())
                },
                successBlock = {
                    val createdAt = nowInUtc()
                    favoriteDao.upsertList(
                        addedMarketIds.map { marketId ->
                            PerpsFavorite(
                                marketId = marketId,
                                isFavored = true,
                                createdAt = createdAt,
                            )
                        },
                    )
                    addedMarketIds
                },
                failureBlock = { true },
                exceptionBlock = { true },
                defaultErrorHandle = {},
                defaultExceptionHandle = {},
                requestSession = {
                    userService.fetchSessionsSuspend(listOf(Constants.RouteConfig.ROUTE_BOT_USER_ID))
                },
            ) ?: emptySet()
        }

        private suspend fun fetchMarkets(category: String? = null): List<PerpsMarket>? =
            requestRouteAPI(
                invokeNetwork = { routeService.getPerpsMarkets(category) },
                successBlock = { response ->
                    response.data.orEmpty().map(PerpsMarket::withDefaults)
                },
                failureBlock = { true },
                exceptionBlock = { true },
                defaultErrorHandle = {},
                defaultExceptionHandle = {},
                requestSession = {
                    userService.fetchSessionsSuspend(listOf(Constants.RouteConfig.ROUTE_BOT_USER_ID))
                },
            )

        private companion object {
            const val CATEGORY_FAVORITE = "favorite"
        }
    }
