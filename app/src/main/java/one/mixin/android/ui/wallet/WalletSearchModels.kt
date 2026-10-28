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
