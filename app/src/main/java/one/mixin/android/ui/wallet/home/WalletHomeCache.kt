package one.mixin.android.ui.wallet.home

import android.content.SharedPreferences
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.extension.putString
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.safe.TokenItem

private const val PREF_WALLET_HOME_CACHE_PREFIX = "pref_wallet_home_cache"

data class WalletHomeCache(
    val walletType: WalletHomeType,
    val fiatTotal: String,
    val btcTotal: String,
    val fiatSymbol: String,
    val privacyTokens: List<TokenItem> = emptyList(),
    val web3Tokens: List<Web3TokenItem> = emptyList(),
    val privacyTransactions: List<SnapshotItem> = emptyList(),
    val web3Transactions: List<Web3TransactionItem> = emptyList(),
    val totalTokenCount: Int,
    val totalTransactionCount: Int,
    val isWatchWallet: Boolean = false,
)

fun walletHomeCacheKey(
    walletType: WalletHomeType,
    walletId: String,
) = "$PREF_WALLET_HOME_CACHE_PREFIX:${walletType.name}:$walletId"

fun SharedPreferences.getWalletHomeCacheState(
    key: String,
): WalletHomeState? =
    runCatching {
        getString(key, null)
            ?.let { GsonHelper.customGson.fromJson(it, WalletHomeCache::class.java) }
            ?.toState()
    }.getOrNull()

fun SharedPreferences.putWalletHomeCache(
    key: String,
    state: WalletHomeState,
) {
    if (state.totalTokenCount == 0 && state.totalTransactionCount == 0) return
    val cache = WalletHomeCache(
        walletType = state.walletType,
        fiatTotal = state.fiatTotal,
        btcTotal = state.btcTotal,
        fiatSymbol = state.fiatSymbol,
        privacyTokens = state.privacyTokens.take(WalletHomeSection.PREVIEW_LIMIT),
        web3Tokens = state.web3Tokens.take(WalletHomeSection.PREVIEW_LIMIT),
        privacyTransactions = state.privacyTransactions.take(WalletHomeSection.PREVIEW_LIMIT),
        web3Transactions = state.web3Transactions.take(WalletHomeSection.PREVIEW_LIMIT),
        totalTokenCount = state.totalTokenCount,
        totalTransactionCount = state.totalTransactionCount,
        isWatchWallet = state.isWatchWallet,
    )
    putString(key, GsonHelper.customGson.toJson(cache))
}

private fun WalletHomeCache.toState(): WalletHomeState {
    val cards = WalletHomeBuilder.build(
        walletType = walletType,
        hasAssetValue = true,
        showBanner = false,
        showReferral = false,
        hasPositions = false,
        hasTopMovers = false,
        hasTransactions = totalTransactionCount > 0,
        isLoading = false,
    )
    return WalletHomeState(
        walletType = walletType,
        cards = cards,
        isLoading = false,
        fiatTotal = fiatTotal,
        btcTotal = btcTotal,
        fiatSymbol = fiatSymbol,
        privacyTokens = privacyTokens,
        web3Tokens = web3Tokens,
        privacyTransactions = privacyTransactions,
        web3Transactions = web3Transactions,
        totalTokenCount = totalTokenCount,
        totalTransactionCount = totalTransactionCount,
        isWatchWallet = isWatchWallet,
        showImportSafetyFooter = false,
    )
}
