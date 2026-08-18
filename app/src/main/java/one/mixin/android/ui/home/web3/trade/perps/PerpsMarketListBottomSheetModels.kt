package one.mixin.android.ui.home.web3.trade.perps

import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.ui.home.web3.market.MarketSortColumn
import one.mixin.android.ui.home.web3.market.MarketSortDirection
import one.mixin.android.ui.home.web3.market.MarketSortState
import one.mixin.android.ui.home.web3.market.PerpsMarketCategoryKey
import one.mixin.android.ui.home.web3.market.changePercentValue
import one.mixin.android.ui.home.web3.widget.MarketSort
import java.math.BigDecimal

internal enum class PerpsMarketCategory(val categoryKey: PerpsMarketCategoryKey?) {
    ALL(null),
    WATCHLIST(null),
    CRYPTO(PerpsMarketCategoryKey.CRYPTO),
    STOCKS(PerpsMarketCategoryKey.STOCKS),
    MEME(PerpsMarketCategoryKey.MEME),
    INDICES(PerpsMarketCategoryKey.INDICES),
    COMMODITIES(PerpsMarketCategoryKey.COMMODITIES),
    FOREX(PerpsMarketCategoryKey.FOREX),
    ;

    val databaseValue: String?
        get() = categoryKey?.databaseValue

    companion object {
        fun fromInitialCategory(category: String?): PerpsMarketCategory =
            entries.firstOrNull { it.categoryKey?.matches(category.orEmpty()) == true } ?: ALL
    }
}

internal data class PerpsMarketListUiState(
    val query: String = "",
    val selectedCategory: PerpsMarketCategory = PerpsMarketCategory.ALL,
    val sortState: MarketSortState = defaultPerpsMarketSortState(PerpsMarketCategory.ALL),
    val markets: List<PerpsMarket> = emptyList(),
    val favoriteMarketIds: Set<String> = emptySet(),
    val favoriteOverrides: Map<String, Boolean> = emptyMap(),
    val pendingFavoriteMarketIds: Set<String> = emptySet(),
    val featuredMarkets: List<PerpsMarket> = emptyList(),
    val selectedRecommendationIds: Set<String> = emptySet(),
    val recommendationSelectionInitialized: Boolean = false,
    val isAddingRecommendations: Boolean = false,
    val quoteColorReversed: Boolean = false,
    val scrollToTopRequest: Int = 0,
) {
    val visibleMarkets: List<PerpsMarket> by lazy {
        markets
            .asSequence()
            .filter(::matchesSelectedCategory)
            .filter(::matchesQuery)
            .toList()
            .sortedByCurrentState()
    }

    val recommendations: List<PerpsMarket> by lazy {
        featuredMarkets.filterNot { it.marketId in favoriteMarketIds }.take(MAX_RECOMMENDATIONS)
    }

    val isShowingRecommendations: Boolean by lazy {
        selectedCategory == PerpsMarketCategory.WATCHLIST &&
            query.isBlank() &&
            recommendations.isNotEmpty() &&
            markets.none { market -> market.marketId in favoriteMarketIds }
    }

    val selectedRecommendations: List<PerpsMarket>
        get() = recommendations.filter { it.marketId in selectedRecommendationIds }

    fun updateQuery(query: String): PerpsMarketListUiState {
        if (this.query == query) return this
        return copy(query = query).normalizeRecommendationSelection()
    }

    fun selectCategory(category: PerpsMarketCategory): PerpsMarketListUiState {
        if (selectedCategory == category) return this
        return copy(
            selectedCategory = category,
            sortState = defaultPerpsMarketSortState(category),
            scrollToTopRequest = scrollToTopRequest + 1,
        ).normalizeRecommendationSelection()
    }

    fun selectSort(column: MarketSortColumn): PerpsMarketListUiState =
        copy(
            sortState = sortState.next(column),
            scrollToTopRequest = scrollToTopRequest + 1,
        ).normalizeRecommendationSelection()

    fun updateMarkets(markets: List<PerpsMarket>): PerpsMarketListUiState =
        if (this.markets == markets) this else copy(markets = markets).normalizeRecommendationSelection()

    fun updateFavoriteMarketIds(marketIds: Set<String>): PerpsMarketListUiState {
        val remainingOverrides =
            favoriteOverrides.filter { (marketId, desiredState) ->
                (marketId in marketIds) != desiredState
            }
        val remainingPendingMarketIds = pendingFavoriteMarketIds intersect remainingOverrides.keys
        if (
            favoriteMarketIds == marketIds &&
            favoriteOverrides == remainingOverrides &&
            pendingFavoriteMarketIds == remainingPendingMarketIds
        ) {
            return this
        }
        return copy(
            favoriteMarketIds = marketIds,
            favoriteOverrides = remainingOverrides,
            pendingFavoriteMarketIds = remainingPendingMarketIds,
        ).normalizeRecommendationSelection()
    }

    fun isFavorite(marketId: String): Boolean =
        favoriteOverrides[marketId] ?: (marketId in favoriteMarketIds)

    fun isFavoriteUpdatePending(marketId: String): Boolean =
        marketId in pendingFavoriteMarketIds

    fun startFavoriteUpdate(marketId: String): PerpsMarketListUiState {
        if (isFavoriteUpdatePending(marketId)) return this
        return copy(
            favoriteOverrides = favoriteOverrides + (marketId to !isFavorite(marketId)),
            pendingFavoriteMarketIds = pendingFavoriteMarketIds + marketId,
        ).normalizeRecommendationSelection()
    }

    fun finishFavoriteUpdate(
        marketId: String,
        success: Boolean,
    ): PerpsMarketListUiState =
        if (success) {
            this
        } else {
            copy(
                favoriteOverrides = favoriteOverrides - marketId,
                pendingFavoriteMarketIds = pendingFavoriteMarketIds - marketId,
            ).normalizeRecommendationSelection()
        }

    fun updateFeaturedMarkets(markets: List<PerpsMarket>): PerpsMarketListUiState =
        if (featuredMarkets == markets) this else copy(featuredMarkets = markets).normalizeRecommendationSelection()

    fun toggleRecommendation(marketId: String): PerpsMarketListUiState {
        if (marketId !in recommendations.mapTo(mutableSetOf()) { it.marketId }) return this
        val selectedIds =
            if (marketId in selectedRecommendationIds) {
                selectedRecommendationIds - marketId
            } else {
                selectedRecommendationIds + marketId
            }
        return copy(selectedRecommendationIds = selectedIds)
    }

    fun startRecommendationSubmission(): PerpsMarketListUiState {
        if (selectedRecommendationIds.isEmpty() || isAddingRecommendations) return this
        return copy(isAddingRecommendations = true)
    }

    fun completeRecommendationSubmission(addedMarketIds: Set<String>): PerpsMarketListUiState =
        copy(
            selectedRecommendationIds = selectedRecommendationIds - addedMarketIds,
            isAddingRecommendations = false,
        ).normalizeRecommendationSelection()

    private fun matchesSelectedCategory(market: PerpsMarket): Boolean =
        when (selectedCategory) {
            PerpsMarketCategory.WATCHLIST -> market.marketId in favoriteMarketIds
            else -> selectedCategory.categoryKey?.matches(market.category) ?: true
        }

    private fun matchesQuery(market: PerpsMarket): Boolean {
        val normalizedQuery = query.trim()
        return normalizedQuery.isEmpty() ||
            market.tokenSymbol.contains(normalizedQuery, ignoreCase = true) ||
            market.tags.any { it.contains(normalizedQuery, ignoreCase = true) }
    }

    private fun List<PerpsMarket>.sortedByCurrentState(): List<PerpsMarket> {
        val column = sortState.column
        if (column == null || sortState.direction == MarketSortDirection.DEFAULT) {
            return if (selectedCategory == PerpsMarketCategory.ALL) {
                sortedByDescending(PerpsMarket::tradeVolumeScore1D)
            } else {
                this
            }
        }
        val comparator =
            compareBy<PerpsMarket> { market ->
                when (column) {
                    MarketSortColumn.VOLUME -> market.volume.toBigDecimalOrNull()
                    MarketSortColumn.PRICE -> market.last.toBigDecimalOrNull()
                    MarketSortColumn.CHANGE -> market.changePercentValue()
                } ?: BigDecimal.ZERO
            }
        return if (sortState.direction == MarketSortDirection.ASCENDING) {
            sortedWith(comparator)
        } else {
            sortedWith(comparator.reversed())
        }
    }

    private fun normalizeRecommendationSelection(): PerpsMarketListUiState {
        if (!isShowingRecommendations) {
            if (selectedRecommendationIds.isEmpty() && !recommendationSelectionInitialized) return this
            return copy(
                selectedRecommendationIds = emptySet(),
                recommendationSelectionInitialized = false,
            )
        }
        val recommendationIds = recommendations.mapTo(linkedSetOf()) { it.marketId }
        val normalizedSelection =
            if (recommendationSelectionInitialized) {
                selectedRecommendationIds intersect recommendationIds
            } else {
                recommendationIds
            }
        if (recommendationSelectionInitialized && selectedRecommendationIds == normalizedSelection) return this
        return copy(
            selectedRecommendationIds = normalizedSelection,
            recommendationSelectionInitialized = true,
        )
    }

    companion object {
        private const val MAX_RECOMMENDATIONS = 8

        fun initial(
            initialCategory: String?,
            initialSort: MarketSort?,
            quoteColorReversed: Boolean,
        ): PerpsMarketListUiState {
            val selectedCategory = PerpsMarketCategory.fromInitialCategory(initialCategory)
            return PerpsMarketListUiState(
                selectedCategory = selectedCategory,
                sortState = initialSort.toPerpsMarketSortState(selectedCategory),
                quoteColorReversed = quoteColorReversed,
            )
        }
    }
}

internal fun defaultPerpsMarketSortState(category: PerpsMarketCategory) =
    if (category == PerpsMarketCategory.ALL) {
        MarketSortState()
    } else {
        MarketSortState(MarketSortColumn.VOLUME, MarketSortDirection.DESCENDING)
    }

internal fun MarketSort?.toPerpsMarketSortState(category: PerpsMarketCategory): MarketSortState =
    when (this) {
        null, MarketSort.RANK_DESCENDING -> defaultPerpsMarketSortState(category)
        MarketSort.RANK_ASCENDING -> MarketSortState(MarketSortColumn.VOLUME, MarketSortDirection.ASCENDING)
        MarketSort.PRICE_ASCENDING -> MarketSortState(MarketSortColumn.PRICE, MarketSortDirection.ASCENDING)
        MarketSort.PRICE_DESCENDING -> MarketSortState(MarketSortColumn.PRICE, MarketSortDirection.DESCENDING)
        MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_ASCENDING ->
            MarketSortState(MarketSortColumn.CHANGE, MarketSortDirection.ASCENDING)
        MarketSort.TWENTY_FOUR_HOURS_PERCENTAGE_DESCENDING ->
            MarketSortState(MarketSortColumn.CHANGE, MarketSortDirection.DESCENDING)
        MarketSort.SEVEN_DAYS_PERCENTAGE_ASCENDING,
        MarketSort.SEVEN_DAYS_PERCENTAGE_DESCENDING,
        -> MarketSortState()
    }
