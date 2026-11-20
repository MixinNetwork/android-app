package one.mixin.android.ui.wallet

import com.google.gson.Gson
import one.mixin.android.vo.RecentSearch
import one.mixin.android.vo.RecentSearchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class WalletSearchModelsTest {
    @Test
    fun positionSearchMatchesMarketTagsAndSymbol() {
        val tags = listOf("bitcoin", "比特币", "比特幣")
        assertTrue(matchesWalletPosition("BTC", "btc", tags))
        assertTrue(matchesWalletPosition("BITCOIN", "BTC", tags))
        assertTrue(matchesWalletPosition("比特币", "BTC", tags))
        assertTrue(matchesWalletPosition("比特幣", "BTC", tags))
        assertFalse(matchesWalletPosition("eth", "BTC", tags))
        assertFalse(matchesWalletPosition("bitcoin", null, emptyList()))
    }

    @Test
    fun recentTapMovesToFrontAndDeduplicatesByTypeAndId() {
        val searches = listOf(asset("a"), collectible("a"), asset("b"), asset("a"))
        val updated = searches.addWalletRecentSearch(asset("a"))
        assertEquals(listOf("a", "a", "b"), updated.map { it.primaryKey })
        assertEquals(listOf(RecentSearchType.ASSET, RecentSearchType.COLLECTIBLE, RecentSearchType.ASSET), updated.map { it.type })
    }

    @Test
    fun collectibleRecentSurvivesReloadAndDeduplicatesWithoutReplacingAnAsset() {
        val collectible = RecentSearch(RecentSearchType.COLLECTIBLE, primaryKey = "same")
        val searches = listOf(asset("same"), collectible).addWalletRecentSearch(collectible)
        val gson = Gson()
        val restored = gson.fromJson(gson.toJson(searches), Array<RecentSearch>::class.java).toList()
            .mergeWalletRecentSearches(emptyList(), emptyList())
        assertEquals(listOf(RecentSearchType.COLLECTIBLE, RecentSearchType.ASSET), restored.map { it.type })
        assertEquals(listOf("same", "same"), restored.map { it.primaryKey })
        assertEquals(listOf(RecentSearchType.ASSET), restored.mergeWalletRecentSearches(emptyList(), emptyList()) {
            it.type != RecentSearchType.COLLECTIBLE
        }.map { it.type })
    }

    @Test
    fun earnRecentRemainsSeparateFromTheSameAssetAfterReload() {
        val earn = RecentSearch(RecentSearchType.EARN, primaryKey = "btc")
        val searches = listOf(asset("btc"), earn, collectible("p")).addWalletRecentSearch(earn)
        val gson = Gson()
        val restored = gson.fromJson(gson.toJson(searches), Array<RecentSearch>::class.java).toList()
            .mergeWalletRecentSearches(emptyList(), emptyList())
        assertEquals(listOf(RecentSearchType.EARN, RecentSearchType.ASSET, RecentSearchType.COLLECTIBLE), restored.map { it.type })
        assertEquals(listOf("btc", "btc", "p"), restored.map { it.primaryKey })
    }

    @Test
    fun positionsAreNotAddedToRecentSearches() {
        val searches = listOf(asset("btc"))
            .addWalletRecentSearch(RecentSearch(RecentSearchType.POSITION, primaryKey = "position"))
        assertEquals(listOf("btc"), searches.map { it.primaryKey })
    }

    @Test
    fun recentLimitKeepsSixNewestItems() {
        val searches = (1..8).map { asset(it.toString()) }.addWalletRecentSearch(collectible("new"))
        assertEquals(listOf("new", "1", "2", "3", "4", "5"), searches.map { it.primaryKey })
    }

    @Test
    fun mergeAppendsSendBeforeReceiveWithoutReplacingMixedRecentOrder() {
        val searches = listOf(collectible("p"), asset("a")).mergeWalletRecentSearches(
            send = listOf("a", "b", "c"),
            receive = listOf("b", "d", "e", "f"),
        )
        assertEquals(listOf("p", "a", "b", "c", "d", "e"), searches.map { it.primaryKey })
        assertEquals(RecentSearchType.COLLECTIBLE, searches.first().type)
    }

    @Test
    fun missingAssetsAndUnavailableCollectiblesAreRemovedBeforeFillingSixSlots() {
        val searches = listOf(asset("missing"), collectible("closed"), collectible("open"), asset("zero-balance"))
            .mergeWalletRecentSearches(listOf("missing", "a", "b", "c"), listOf("d", "e")) {
                it.primaryKey !in setOf("missing", "closed")
            }
        assertEquals(listOf("open", "zero-balance", "a", "b", "c", "d"), searches.map { it.primaryKey })
    }

    @Test
    fun malformedAndUnrelatedRecentEntriesDoNotConsumeSlots() {
        val searches = listOf(asset(""), RecentSearch(RecentSearchType.MARKET, primaryKey = "market"), collectible("p"))
            .mergeWalletRecentSearches(emptyList(), listOf("a"))
        assertEquals(listOf("p", "a"), searches.map { it.primaryKey })
    }

    @Test
    fun tabsIncludeOnlyNonEmptyGroupsInDesignOrder() {
        assertEquals(listOf(WalletSearchTab.ALL), walletSearchTabs(emptyList()))
        assertEquals(
            listOf(WalletSearchTab.ALL, WalletSearchTab.ASSETS, WalletSearchTab.EARN, WalletSearchTab.COLLECTIBLES),
            walletSearchTabs(listOf(WalletSearchTab.COLLECTIBLES, WalletSearchTab.EARN, WalletSearchTab.ASSETS, WalletSearchTab.ASSETS)),
        )
    }

    @Test
    fun missingSelectedTabFallsBackToAll() {
        val tabs = walletSearchTabs(listOf(WalletSearchTab.ASSETS))
        assertEquals(WalletSearchTab.ALL, selectedWalletSearchTab(WalletSearchTab.POSITIONS, tabs))
        assertEquals(WalletSearchTab.ASSETS, selectedWalletSearchTab(WalletSearchTab.ASSETS, tabs))
    }

    @Test
    fun allCapsEachGroupAtThreeWhileSingleTabKeepsEveryResult() {
        val results = listOf(1, 2, 3, 4)
        assertEquals(listOf(1, 2, 3), walletSearchPreview(results, all = true))
        assertEquals(results, walletSearchPreview(results, all = false))
        assertFalse(walletSearchHasMore(3))
        assertTrue(walletSearchHasMore(4))
        assertEquals(emptyList<Int>(), walletSearchPreview(emptyList<Int>(), all = true))
    }

    @Test
    fun enteringQueryResetsToAllAndEditingKeepsTheSelectedGroup() {
        assertEquals(WalletSearchTab.ALL, walletSearchTabForQuery("", "btc", WalletSearchTab.ASSETS))
        assertEquals(WalletSearchTab.ASSETS, walletSearchTabForQuery("bt", "btc", WalletSearchTab.ASSETS))
        assertEquals(WalletSearchTab.ALL, walletSearchTabForQuery("btc", " ", WalletSearchTab.ASSETS))
    }

    @Test
    fun earnAmountUsesUsdPriceAndRequiresAPositivePrice() {
        assertEquals(BigDecimal("2.50000000"), walletSearchEarnAmount(BigDecimal("100"), BigDecimal("40")))
        assertEquals(BigDecimal("0.33333333"), walletSearchEarnAmount(BigDecimal.ONE, BigDecimal("3")))
        assertEquals(null, walletSearchEarnAmount(BigDecimal.TEN, null))
        assertEquals(null, walletSearchEarnAmount(BigDecimal.TEN, BigDecimal.ZERO))
        assertEquals(null, walletSearchEarnAmount(BigDecimal.TEN, BigDecimal("-1")))
    }

    @Test
    fun web3SearchRecentStartsEmptyWithoutSharedHistory() {
        assertEquals(emptyList<String>(), web3WalletSearchRecentIds(null, setOf("btc", "eth")))
    }

    @Test
    fun web3SearchRecentPreservesSharedOrderAndFiltersUnavailableTokens() {
        assertEquals(
            listOf("btc", "eth", "sol"),
            web3WalletSearchRecentIds(
                storedIds = listOf("btc", "", "missing", "eth", "btc", "sol", "usdt"),
                availableIds = setOf("btc", "eth", "sol", "usdt"),
                limit = 3,
            ),
        )
    }

    @Test
    fun wideRecentChipsShareHalfTheRow() {
        val rows = walletRecentChipRows(listOf(800, 900), contentWidth = 1000, gap = 40)
        assertEquals(listOf(480, 480), rows.single().map { it.width })
    }

    @Test
    fun wideRecentChipGrowsPastHalfBesideAShortChip() {
        val rows = walletRecentChipRows(listOf(200, 1000), contentWidth = 1000, gap = 40)
        assertEquals(listOf(200, 760), rows.single().map { it.width })
    }

    @Test
    fun wideRecentChipUsesTheRemainingRowWhenShortChipsLeaveLessThanHalf() {
        val rows = walletRecentChipRows(listOf(300, 300, 1000), contentWidth = 1000, gap = 40)
        assertEquals(listOf(300, 300), rows[0].map { it.width })
        assertEquals(listOf(1000), rows[1].map { it.width })
    }

    @Test
    fun shortRecentChipsKeepTheirOwnWidth() {
        val rows = walletRecentChipRows(listOf(100, 120, 140), contentWidth = 1000, gap = 40)
        assertEquals(listOf(100, 120, 140), rows.single().map { it.width })
    }

    @Test
    fun recentChipRowsStopAtThreeLines() {
        val rows = walletRecentChipRows(List(7) { 800 }, contentWidth = 1000, gap = 40)
        assertEquals(3, rows.size)
        assertEquals(listOf(0, 1, 2, 3, 4, 5), rows.flatten().map { it.index })
    }

    private fun asset(id: String) = RecentSearch(RecentSearchType.ASSET, primaryKey = id)
    private fun collectible(id: String) = RecentSearch(RecentSearchType.COLLECTIBLE, primaryKey = id)
}
