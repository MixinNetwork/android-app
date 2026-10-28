package one.mixin.android.ui.wallet

import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.ui.wallet.home.WalletHomeEarnAccount
import one.mixin.android.vo.RecentSearch
import one.mixin.android.vo.RecentSearchType
import one.mixin.android.vo.safe.SafeCollectible
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal
import java.math.RoundingMode

internal const val WALLET_SEARCH_PREVIEW_COUNT = 3
internal const val MAX_WALLET_RECENT_SEARCHES = 6

internal fun web3WalletSearchRecentIds(
    storedIds: List<String>?,
    sendIds: List<String>,
    receiveIds: List<String>,
    availableIds: Set<String>,
    limit: Int = MAX_WALLET_RECENT_SEARCHES,
): List<String> {
    val source = storedIds ?: (sendIds + receiveIds)
    return source.map { it.trim() }.filter { it.isNotEmpty() && it in availableIds }.distinct().take(limit)
}

internal enum class WalletSearchTab { ALL, ASSETS, POSITIONS, EARN, COLLECTIBLES }

internal sealed interface WalletSearchResult {
    val tab: WalletSearchTab

    data class Asset(val token: TokenItem) : WalletSearchResult {
        override val tab = WalletSearchTab.ASSETS
    }

    data class Position(val position: PerpsPositionItem, val tokenName: String) : WalletSearchResult {
        override val tab = WalletSearchTab.POSITIONS
    }

    data class Earn(val account: WalletHomeEarnAccount, val amount: String) : WalletSearchResult {
        override val tab = WalletSearchTab.EARN
    }

    data class Collectible(val collectible: SafeCollectible) : WalletSearchResult {
        override val tab = WalletSearchTab.COLLECTIBLES
    }
}

internal data class WalletSearchUiState(
    val query: String = "",
    val selectedTab: WalletSearchTab = WalletSearchTab.ALL,
    val results: List<WalletSearchResult> = emptyList(),
    val isSearching: Boolean = false,
)

internal fun walletSearchTabs(groups: Collection<WalletSearchTab>): List<WalletSearchTab> =
    WalletSearchTab.entries.filter { it == WalletSearchTab.ALL || it in groups }

internal fun selectedWalletSearchTab(selected: WalletSearchTab, tabs: List<WalletSearchTab>): WalletSearchTab =
    selected.takeIf { it in tabs } ?: WalletSearchTab.ALL

internal fun walletSearchTabForQuery(previousQuery: String, query: String, selected: WalletSearchTab): WalletSearchTab =
    if (previousQuery.isBlank() || query.isBlank()) WalletSearchTab.ALL else selected

internal fun walletSearchEarnAmount(balanceUsd: BigDecimal, priceUsd: BigDecimal?): BigDecimal? =
    priceUsd?.takeIf { it > BigDecimal.ZERO }?.let { balanceUsd.divide(it, 8, RoundingMode.DOWN) }

internal fun <T> walletSearchPreview(results: List<T>, all: Boolean): List<T> =
    if (all) results.take(WALLET_SEARCH_PREVIEW_COUNT) else results

internal fun walletSearchHasMore(count: Int): Boolean = count > WALLET_SEARCH_PREVIEW_COUNT

internal fun List<RecentSearch>.addWalletRecentSearch(search: RecentSearch): List<RecentSearch> =
    (listOf(search) + this).mergeWalletRecentSearches(emptyList(), emptyList())

internal fun List<RecentSearch>.mergeWalletRecentSearches(
    send: List<String>,
    receive: List<String>,
    available: (RecentSearch) -> Boolean = { true },
): List<RecentSearch> =
    (this + (send + receive).map { RecentSearch(RecentSearchType.ASSET, primaryKey = it) })
        .filter { (it.type == RecentSearchType.ASSET || it.type == RecentSearchType.POSITION) && !it.primaryKey.isNullOrBlank() }
        .distinctBy { it.type to it.primaryKey }
        .filter(available)
        .take(MAX_WALLET_RECENT_SEARCHES)

internal data class WalletRecentChipPlacement(val index: Int, val width: Int)

internal fun walletRecentChipRows(
    preferredWidths: List<Int>,
    contentWidth: Int,
    gap: Int,
    maxLines: Int = 3,
): List<List<WalletRecentChipPlacement>> {
    if (contentWidth <= 0 || maxLines <= 0 || preferredWidths.isEmpty()) return emptyList()
    val rows = mutableListOf<List<Int>>()
    var current = mutableListOf<Int>()
    for (index in preferredWidths.indices) {
        if (recentChipRowWidths((current + index).map { preferredWidths[it] }, contentWidth, gap) != null) {
            current.add(index)
            continue
        }
        if (current.isNotEmpty()) rows += current.toList()
        if (rows.size >= maxLines) break
        current = mutableListOf(index)
    }
    if (current.isNotEmpty() && rows.size < maxLines) rows += current.toList()
    return rows.map { indexes ->
        val widths = recentChipRowWidths(indexes.map { preferredWidths[it] }, contentWidth, gap)
            ?: indexes.map { preferredWidths[it].coerceIn(0, contentWidth) }
        indexes.mapIndexed { position, index -> WalletRecentChipPlacement(index, widths[position]) }
    }
}

private fun recentChipRowWidths(preferred: List<Int>, contentWidth: Int, gap: Int): List<Int>? {
    if (preferred.isEmpty()) return emptyList()
    val widths = preferred.map { it.coerceAtLeast(0) }
    if (widths.size == 1) return listOf(widths[0].coerceAtMost(contentWidth))
    val available = contentWidth - gap * (widths.size - 1)
    if (available <= 0) return null
    if (widths.sum() <= available) return widths
    val half = (contentWidth - gap).coerceAtLeast(0) / 2
    val wide = widths.map { it > half }
    val wideCount = wide.count { it }
    if (wideCount == 0 || (wideCount >= 2 && widths.size != 2)) return null
    if (wideCount >= 2) return List(widths.size) { half }
    val wideWidth = available - widths.filterIndexed { index, _ -> !wide[index] }.sum()
    if (wideWidth < half) return null
    return widths.mapIndexed { index, width -> if (wide[index]) width.coerceAtMost(wideWidth) else width }
}
