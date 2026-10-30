package one.mixin.android.ui.wallet

import one.mixin.android.vo.RecentSearch
import one.mixin.android.vo.RecentSearchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class WalletSearchModelsTest {
    @Test
    fun recentTapMovesToFrontAndDeduplicatesByTypeAndId() {
        val searches = listOf(asset("a"), position("a"), asset("b"), asset("a"))
        val updated = searches.addWalletRecentSearch(asset("a"))
        assertEquals(listOf("a", "a", "b"), updated.map { it.primaryKey })
        assertEquals(listOf(RecentSearchType.ASSET, RecentSearchType.POSITION, RecentSearchType.ASSET), updated.map { it.type })
    }

    @Test
    fun recentLimitKeepsSixNewestItems() {
        val searches = (1..8).map { asset(it.toString()) }.addWalletRecentSearch(position("new"))
        assertEquals(listOf("new", "1", "2", "3", "4", "5"), searches.map { it.primaryKey })
    }

    @Test
    fun mergeAppendsSendBeforeReceiveWithoutReplacingMixedRecentOrder() {
        val searches = listOf(position("p"), asset("a")).mergeWalletRecentSearches(
            send = listOf("a", "b", "c"),
            receive = listOf("b", "d", "e", "f"),
        )
        assertEquals(listOf("p", "a", "b", "c", "d", "e"), searches.map { it.primaryKey })
        assertEquals(RecentSearchType.POSITION, searches.first().type)
    }

    @Test
    fun missingAssetsAndClosedPositionsAreRemovedBeforeFillingSixSlots() {
        val searches = listOf(asset("missing"), position("closed"), position("open"), asset("zero-balance"))
            .mergeWalletRecentSearches(listOf("missing", "a", "b", "c"), listOf("d", "e")) {
                it.primaryKey !in setOf("missing", "closed")
            }
        assertEquals(listOf("open", "zero-balance", "a", "b", "c", "d"), searches.map { it.primaryKey })
    }

    @Test
    fun malformedAndUnrelatedRecentEntriesDoNotConsumeSlots() {
        val searches = listOf(asset(""), RecentSearch(RecentSearchType.MARKET, primaryKey = "market"), position("p"))
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
    private fun position(id: String) = RecentSearch(RecentSearchType.POSITION, primaryKey = id)
}
