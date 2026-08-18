package one.mixin.android.ui.home.web3.market

import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.event.MarketPageDataSource
import one.mixin.android.vo.market.MarketItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketPageModelsTest {
    @Test
    fun refreshSourcesMatchVisibleMarketPage() {
        val cases =
            listOf(
                Triple(
                    MarketTopTab.WATCHLIST,
                    MarketSubTab.CRYPTO,
                    setOf(MarketPageDataSource.SPOT_FAVORITE, MarketPageDataSource.SPOT_FEATURED),
                ),
                Triple(
                    MarketTopTab.WATCHLIST,
                    MarketSubTab.PERPETUAL,
                    setOf(MarketPageDataSource.PERPETUAL_FAVORITE, MarketPageDataSource.PERPETUAL_FEATURED),
                ),
                Triple(
                    MarketTopTab.CRYPTO,
                    MarketSubTab.FAVORITE,
                    setOf(MarketPageDataSource.SPOT_FAVORITE, MarketPageDataSource.SPOT_FEATURED),
                ),
                Triple(
                    MarketTopTab.CRYPTO,
                    MarketSubTab.TRENDING,
                    setOf(MarketPageDataSource.SPOT_TRENDING),
                ),
                Triple(
                    MarketTopTab.CRYPTO,
                    MarketSubTab.TOP_GAINERS,
                    setOf(MarketPageDataSource.SPOT_TOP_GAINER),
                ),
                Triple(
                    MarketTopTab.CRYPTO,
                    MarketSubTab.TOP_LOSERS,
                    setOf(MarketPageDataSource.SPOT_TOP_LOSER),
                ),
                Triple(
                    MarketTopTab.CRYPTO,
                    MarketSubTab.ALL,
                    setOf(MarketPageDataSource.SPOT_ALL),
                ),
                Triple(
                    MarketTopTab.PERPETUAL,
                    MarketSubTab.FAVORITE,
                    setOf(MarketPageDataSource.PERPETUAL_FAVORITE, MarketPageDataSource.PERPETUAL_FEATURED),
                ),
                Triple(
                    MarketTopTab.PERPETUAL,
                    MarketSubTab.INDICES,
                    setOf(MarketPageDataSource.PERPETUAL_ALL),
                ),
                Triple(
                    MarketTopTab.INDICATOR,
                    null,
                    setOf(MarketPageDataSource.GLOBAL),
                ),
            )

        cases.forEach { (topTab, subTab, sources) ->
            assertEquals(sources, marketPageRefreshSources(topTab, subTab))
        }
    }

    @Test
    fun defaultSelectionStartsAtExpectedSubTabs() {
        val defaults = defaultMarketSubTabs()

        assertEquals(MarketSubTab.CRYPTO, defaults[MarketTopTab.WATCHLIST])
        assertEquals(MarketSubTab.TRENDING, defaults[MarketTopTab.CRYPTO])
        assertEquals(MarketSubTab.TRENDING, defaults[MarketTopTab.PERPETUAL])
        assertEquals(
            listOf(MarketTopTab.WATCHLIST, MarketTopTab.CRYPTO, MarketTopTab.PERPETUAL, MarketTopTab.INDICATOR),
            MarketTopTab.entries,
        )
    }

    @Test
    fun perpetualSubTabsDoNotIncludeAll() {
        assertEquals(
            listOf(
                MarketSubTab.FAVORITE,
                MarketSubTab.TRENDING,
                MarketSubTab.TOP_GAINERS,
                MarketSubTab.TOP_LOSERS,
                MarketSubTab.MEME,
                MarketSubTab.INDICES,
                MarketSubTab.COMMODITIES,
                MarketSubTab.FOREX,
            ),
            marketSubTabs(MarketTopTab.PERPETUAL),
        )
    }

    @Test
    fun watchlistSpotIncludesAllSpotMarketTypes() {
        val crypto = market(coinId = "btc", perpsMarketId = "btc-perp", favored = true)
        val stock = market(coinId = "hood", favored = true)
        val perpetual = perpsMarket(marketId = "btc-perp")

        val result =
            MarketPageMapper.watchlist(
                spotFavorites = listOf(crypto, stock),
                perpetualFavorites = listOf(perpetual),
                stockCoinIds = setOf("hood"),
                subTab = MarketSubTab.ALL,
            )

        assertEquals(listOf("spot:btc", "spot:hood"), result.map { it.stableId })
        assertEquals(
            listOf(SpotMarketType.CRYPTO, SpotMarketType.STOCK),
            result.map { (it as MarketListEntry.Spot).type },
        )
    }

    @Test
    fun watchlistPerpetualUsesIndependentFavorites() {
        val spotFavorite = market(coinId = "eth", perpsMarketId = "eth-perp", favored = true)
        val perpetualFavorite = perpsMarket("btc-perp")

        val result =
            MarketPageMapper.watchlist(
                spotFavorites = listOf(spotFavorite),
                perpetualFavorites = listOf(perpetualFavorite),
                stockCoinIds = emptySet(),
                subTab = MarketSubTab.PERPETUAL,
            )

        assertEquals(listOf("perpetual:btc-perp"), result.map { it.stableId })
        assertTrue(result.single().isFavored)
    }

    @Test
    fun emptyCryptoWatchlistUsesFeaturedMarkets() {
        val featured = market(coinId = "btc")

        val result =
            MarketPageMapper.watchlist(
                spotFavorites = emptyList(),
                perpetualFavorites = emptyList(),
                stockCoinIds = emptySet(),
                subTab = MarketSubTab.CRYPTO,
                spotFeatured = listOf(featured),
            )

        assertEquals(listOf("spot:btc"), result.map { it.stableId })
        assertTrue(!result.single().isFavored)
    }

    @Test
    fun emptyPerpetualWatchlistUsesFeaturedMarkets() {
        val featured = perpsMarket("btc-perp")

        val result =
            MarketPageMapper.watchlist(
                spotFavorites = emptyList(),
                perpetualFavorites = emptyList(),
                stockCoinIds = emptySet(),
                subTab = MarketSubTab.PERPETUAL,
                perpetualFeatured = listOf(featured),
            )

        assertEquals(listOf("perpetual:btc-perp"), result.map { it.stableId })
        assertTrue(!result.single().isFavored)
    }

    @Test
    fun perpetualChangeAlwaysUsesTwentyFourHourData() {
        val market = perpsMarket(marketId = "btc-perp", change = "0.12")
        val entry = MarketListEntry.Perpetual(market, false)

        assertEquals("12.00", entry.changePercent(MarketPriceChangePeriod.SEVEN_DAYS)?.toPlainString())
        assertEquals("12.00", entry.changePercent(MarketPriceChangePeriod.TWENTY_FOUR_HOURS)?.toPlainString())
    }

    @Test
    fun perpetualGainersAndLosersSortAllMarketsLocally() {
        val markets =
            listOf(
                perpsMarket(marketId = "unchanged", change = "0"),
                perpsMarket(marketId = "loser", change = "-0.08"),
                perpsMarket(marketId = "gainer", change = "0.12"),
            )

        val gainers = MarketPageMapper.perpetualMarkets(markets, MarketSubTab.TOP_GAINERS)
        val losers = MarketPageMapper.perpetualMarkets(markets, MarketSubTab.TOP_LOSERS)

        assertEquals(listOf("gainer", "unchanged", "loser"), gainers.map { it.marketId })
        assertEquals(listOf("loser", "unchanged", "gainer"), losers.map { it.marketId })
    }

    @Test
    fun perpetualTrendingSortsByTradeVolumeScore() {
        val markets =
            listOf(
                perpsMarket(marketId = "second", tradeVolumeScore1D = 20),
                perpsMarket(marketId = "third", tradeVolumeScore1D = 10),
                perpsMarket(marketId = "first", tradeVolumeScore1D = 30),
            )

        val trending = MarketPageMapper.perpetualMarkets(markets, MarketSubTab.TRENDING)

        assertEquals(listOf("first", "second", "third"), trending.map { it.marketId })
    }

    @Test
    fun perpetualCategoriesSupportAliasesIgnoringCase() {
        val markets =
            listOf(
                perpsMarket(marketId = "indices", category = "indices"),
                perpsMarket(marketId = "index", category = "INDEX"),
                perpsMarket(marketId = "commodities", category = "commodities"),
                perpsMarket(marketId = "commodity", category = "Commodity"),
                perpsMarket(marketId = "forex", category = "forex"),
                perpsMarket(marketId = "fx", category = "FX"),
                perpsMarket(marketId = "memes", category = "memes"),
                perpsMarket(marketId = "meme", category = "Meme"),
                perpsMarket(marketId = "crypto", category = "crypto"),
            )

        assertEquals(
            listOf("indices", "index"),
            MarketPageMapper.perpetualMarkets(markets, MarketSubTab.INDICES).map { it.marketId },
        )
        assertEquals(
            listOf("commodities", "commodity"),
            MarketPageMapper.perpetualMarkets(markets, MarketSubTab.COMMODITIES).map { it.marketId },
        )
        assertEquals(
            listOf("forex", "fx"),
            MarketPageMapper.perpetualMarkets(markets, MarketSubTab.FOREX).map { it.marketId },
        )
        assertEquals(
            listOf("memes", "meme"),
            MarketPageMapper.perpetualMarkets(markets, MarketSubTab.MEME).map { it.marketId },
        )
    }

    @Test
    fun spotGainersAndLosersSortBySelectedPeriod() {
        val markets =
            listOf(
                market(coinId = "middle", change24h = "2", change7d = "-2"),
                market(coinId = "winner", change24h = "8", change7d = "-8"),
                market(coinId = "loser", change24h = "-4", change7d = "4"),
            )

        val gainers =
            MarketPageMapper.spotMarkets(
                markets = markets,
                subTab = MarketSubTab.TOP_GAINERS,
                period = MarketPriceChangePeriod.TWENTY_FOUR_HOURS,
            )
        val losers =
            MarketPageMapper.spotMarkets(
                markets = markets,
                subTab = MarketSubTab.TOP_LOSERS,
                period = MarketPriceChangePeriod.SEVEN_DAYS,
            )

        assertEquals(listOf("winner", "middle", "loser"), gainers.map { it.coinId })
        assertEquals(listOf("winner", "middle", "loser"), losers.map { it.coinId })
    }

    @Test
    fun spotCategoryUsesAllMarketCacheUntilCategoryCacheLoads() {
        val cachedMarkets = listOf(market("btc"), market("eth"))

        val result =
            MarketPageMapper.spotMarkets(
                markets = emptyList(),
                fallbackMarkets = cachedMarkets,
                subTab = MarketSubTab.TRENDING,
                period = MarketPriceChangePeriod.SEVEN_DAYS,
            )

        assertEquals(listOf("btc", "eth"), result.map { it.coinId })
    }

    @Test
    fun perpetualOnlySelectionForcesTwentyFourHourDisplay() {
        val state =
            MarketPageUiState(
                selectedTopTab = MarketTopTab.PERPETUAL,
                displaySettings =
                    MarketDisplaySettings(
                        priceChangePeriod = MarketPriceChangePeriod.SEVEN_DAYS,
                    ),
            )

        assertTrue(state.showsOnlyPerpetualMarkets)
        assertEquals(MarketPriceChangePeriod.TWENTY_FOUR_HOURS, state.effectivePriceChangePeriod)
    }

    @Test
    fun sortHeaderCyclesDescendingAscendingAndDefault() {
        val descending = MarketSortState().next(MarketSortColumn.PRICE)
        val ascending = descending.next(MarketSortColumn.PRICE)
        val reset = ascending.next(MarketSortColumn.PRICE)

        assertEquals(MarketSortDirection.DESCENDING, descending.direction)
        assertEquals(MarketSortDirection.ASCENDING, ascending.direction)
        assertEquals(MarketSortState(), reset)
    }

    @Test
    fun defaultVolumeSortMatchesMarketCategory() {
        listOf(
            MarketSubTab.CRYPTO,
            MarketSubTab.PERPETUAL,
        ).forEach { subTab ->
            assertEquals(
                MarketSortState(MarketSortColumn.VOLUME, MarketSortDirection.DESCENDING),
                defaultMarketSortState(MarketTopTab.WATCHLIST, subTab),
            )
        }
        assertEquals(
            MarketSortState(MarketSortColumn.VOLUME, MarketSortDirection.DESCENDING),
            defaultMarketSortState(MarketTopTab.CRYPTO, MarketSubTab.ALL),
        )
        assertEquals(
            MarketSortState(),
            defaultMarketSortState(MarketTopTab.PERPETUAL, MarketSubTab.TRENDING),
        )
        assertEquals(
            MarketSortState(MarketSortColumn.VOLUME, MarketSortDirection.DESCENDING),
            defaultMarketSortState(MarketTopTab.CRYPTO, MarketSubTab.TRENDING),
        )
        assertEquals(
            MarketSortState(MarketSortColumn.CHANGE, MarketSortDirection.DESCENDING),
            defaultMarketSortState(MarketTopTab.CRYPTO, MarketSubTab.TOP_GAINERS),
        )
        assertEquals(
            MarketSortState(MarketSortColumn.CHANGE, MarketSortDirection.ASCENDING),
            defaultMarketSortState(MarketTopTab.CRYPTO, MarketSubTab.TOP_LOSERS),
        )
        assertEquals(
            MarketSortState(MarketSortColumn.VOLUME, MarketSortDirection.DESCENDING),
            defaultMarketSortState(MarketTopTab.PERPETUAL, MarketSubTab.MEME),
        )
        listOf(
            MarketSubTab.INDICES,
            MarketSubTab.COMMODITIES,
            MarketSubTab.FOREX,
        ).forEach { subTab ->
            assertEquals(
                MarketSortState(MarketSortColumn.VOLUME, MarketSortDirection.DESCENDING),
                defaultMarketSortState(MarketTopTab.PERPETUAL, subTab),
            )
        }
    }

    @Test
    fun marketFavoritesDefaultToVolumeDescending() {
        listOf(MarketTopTab.CRYPTO, MarketTopTab.PERPETUAL).forEach { topTab ->
            assertEquals(
                MarketSortState(MarketSortColumn.VOLUME, MarketSortDirection.DESCENDING),
                defaultMarketSortState(topTab, MarketSubTab.FAVORITE),
            )
        }
    }

    @Test
    fun spotAllVolumeColumnSortsByMarketCap() {
        val lowerMarketCap =
            MarketListEntry.Spot(
                market(coinId = "higher-volume", marketCap = "100", totalVolume = "1000"),
                SpotMarketType.CRYPTO,
            )
        val higherMarketCap =
            MarketListEntry.Spot(
                market(coinId = "lower-volume", marketCap = "200", totalVolume = "10"),
                SpotMarketType.CRYPTO,
            )

        val result =
            MarketPageMapper.applySort(
                entries = listOf(lowerMarketCap, higherMarketCap),
                sortState = MarketSortState(MarketSortColumn.VOLUME, MarketSortDirection.DESCENDING),
                period = MarketPriceChangePeriod.SEVEN_DAYS,
                useMarketCapForSpot = true,
            )

        assertEquals(listOf("spot:lower-volume", "spot:higher-volume"), result.map { it.stableId })
    }

    @Test
    fun spotNonAllVolumeColumnSortsByTradingVolume() {
        val higherVolume =
            MarketListEntry.Spot(
                market(coinId = "higher-volume", marketCap = "100", totalVolume = "1000"),
                SpotMarketType.CRYPTO,
            )
        val lowerVolume =
            MarketListEntry.Spot(
                market(coinId = "lower-volume", marketCap = "200", totalVolume = "10"),
                SpotMarketType.CRYPTO,
            )

        val result =
            MarketPageMapper.applySort(
                entries = listOf(lowerVolume, higherVolume),
                sortState = MarketSortState(MarketSortColumn.VOLUME, MarketSortDirection.DESCENDING),
                period = MarketPriceChangePeriod.SEVEN_DAYS,
                useMarketCapForSpot = false,
            )

        assertEquals(listOf("spot:higher-volume", "spot:lower-volume"), result.map { it.stableId })
    }

    @Test
    fun initialLoadingDoesNotFlashSpinnerBeforeLocalDatabaseResult() {
        val state = MarketPageUiState(isLoading = true, hasLoadedLocalData = false)

        assertTrue(!state.showsMarketLoading)
    }

    @Test
    fun initialLoadingShowsWhenLocalDatabaseIsEmpty() {
        val state = MarketPageUiState(isLoading = true, hasLoadedLocalData = true)

        assertTrue(state.showsMarketLoading)
    }

    @Test
    fun initialLoadingSkipsSpinnerWhenLocalDatabaseHasEntries() {
        val state =
            MarketPageUiState(
                entries = listOf(MarketListEntry.Spot(market("btc"), SpotMarketType.CRYPTO)),
                isLoading = true,
                hasLoadedLocalData = true,
            )

        assertTrue(!state.showsMarketLoading)
    }

    private fun market(
        coinId: String,
        change24h: String = "0",
        change7d: String = "0",
        perpsMarketId: String? = null,
        favored: Boolean = false,
        marketCap: String = "100",
        totalVolume: String = "10",
    ) = MarketItem(
        coinId = coinId,
        name = coinId,
        symbol = coinId.uppercase(),
        iconUrl = "",
        currentPrice = "1",
        marketCap = marketCap,
        marketCapRank = "1",
        totalVolume = totalVolume,
        high24h = "1",
        low24h = "1",
        priceChange24h = "0",
        priceChangePercentage1H = "0",
        priceChangePercentage24H = change24h,
        priceChangePercentage7D = change7d,
        priceChangePercentage30D = "0",
        marketCapChange24h = "0",
        marketCapChangePercentage24h = "0",
        circulatingSupply = "0",
        totalSupply = "0",
        maxSupply = "0",
        ath = "0",
        athChangePercentage = "0",
        athDate = "",
        atl = "0",
        atlChangePercentage = "0",
        atlDate = "",
        assetIds = emptyList(),
        sparklineIn7d = "",
        sparklineIn24 = "",
        isFavored = favored,
        perpsMarketId = perpsMarketId,
    )

    private fun perpsMarket(
        marketId: String,
        change: String = "0",
        category: String = "",
        tradeVolumeScore1D: Int = 0,
    ) = PerpsMarket(
        marketId = marketId,
        displaySymbol = marketId,
        tokenSymbol = "BTC",
        quoteSymbol = "USD",
        markPrice = "1",
        leverage = 10,
        iconUrl = "",
        category = category,
        fundingRate = "0",
        minAmount = "0",
        maxAmount = "0",
        last = "1",
        volume = "10",
        tradeVolumeScore1D = tradeVolumeScore1D,
        high = "1",
        low = "1",
        open = "1",
        change = change,
        bidPrice = "1",
        askPrice = "1",
        createdAt = "",
        updatedAt = "",
    )
}
