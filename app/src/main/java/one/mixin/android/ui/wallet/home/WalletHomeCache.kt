package one.mixin.android.ui.wallet.home

import android.content.SharedPreferences
import com.google.gson.annotations.SerializedName
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.extension.putString
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.safe.TokenItem

private const val PREF_WALLET_HOME_CACHE_PREFIX = "pref_wallet_home_cache"

data class WalletHomeCache(
    @SerializedName("walletType")
    val walletType: WalletHomeType,
    @SerializedName("fiatTotal")
    val fiatTotal: String,
    @SerializedName("btcTotal")
    val btcTotal: String,
    @SerializedName("fiatSymbol")
    val fiatSymbol: String,
    @SerializedName("privacyTokens")
    val privacyTokens: List<TokenItem> = emptyList(),
    @SerializedName("web3Tokens")
    val web3Tokens: List<Web3TokenItem> = emptyList(),
    @SerializedName("privacyTransactions")
    val privacyTransactions: List<SnapshotItem> = emptyList(),
    @SerializedName("web3Transactions")
    val web3Transactions: List<Web3TransactionItem> = emptyList(),
    @SerializedName("totalTokenCount")
    val totalTokenCount: Int,
    @SerializedName("totalTransactionCount")
    val totalTransactionCount: Int,
    @SerializedName("cashAccount")
    val cashAccount: WalletHomeCashAccount? = null,
    @SerializedName("isWatchWallet")
    val isWatchWallet: Boolean = false,
    @SerializedName("watchAddresses")
    val watchAddresses: List<String>? = null,
    @SerializedName("pendingIndicator")
    val pendingIndicator: WalletHomePendingIndicator? = null,
    @SerializedName("importKeyAction")
    val importKeyAction: WalletHomeImportKeyAction? = null,
    @SerializedName("importKeyChainId")
    val importKeyChainId: String? = null,
) {

    fun toState(): WalletHomeState {
        val cachedImportKeyAction = importKeyAction
        val cards = WalletHomeBuilder.build(
            walletType = walletType,
            hasAssetValue = true,
            showBanner = false,
            showReferral = false,
            hasPositions = false,
            hasCashAccount = false,
            hasTopMovers = false,
            hasTransactions = totalTransactionCount > 0,
            hasImportKeyAction = cachedImportKeyAction != null,
            hasPendingIndicator = pendingIndicator != null,
            isLoading = false,
        )
        return WalletHomeState(
            walletType = walletType,
            cards = cards,
            isLoading = false,
            fiatTotal = fiatTotal,
            btcTotal = btcTotal,
            fiatSymbol = fiatSymbol,
            privacyTokens = privacyTokens.orEmpty(),
            web3Tokens = web3Tokens.orEmpty(),
            privacyTransactions = privacyTransactions.orEmpty(),
            web3Transactions = web3Transactions.orEmpty(),
            totalTokenCount = totalTokenCount,
            totalTransactionCount = totalTransactionCount,
            isWatchWallet = isWatchWallet,
            pendingIndicator = pendingIndicator,
            watchIndicator = if (isWatchWallet) walletHomeWatchIndicator(watchAddresses.orEmpty()) else null,
            importKeyAction = cachedImportKeyAction,
            showImportSafetyFooter = false,
        )
    }
}

fun walletHomeCacheKey(
    walletType: WalletHomeType,
    walletId: String,
) = "$PREF_WALLET_HOME_CACHE_PREFIX:${walletType.name}:$walletId"

fun SharedPreferences.getWalletHomeCacheState(
    key: String,
): WalletHomeState? =
    getWalletHomeCache(key)?.toState()

fun SharedPreferences.getWalletHomeCache(
    key: String,
): WalletHomeCache? =
    runCatching {
        getString(key, null)
            ?.let { GsonHelper.customGson.fromJson(it, WalletHomeCache::class.java) }
    }.getOrNull()

fun SharedPreferences.putWalletHomeCache(
    key: String,
    state: WalletHomeState,
    watchAddresses: List<String> = emptyList(),
    importKeyChainId: String? = null,
) {
    if (
        state.totalTokenCount == 0 &&
        state.totalTransactionCount == 0 &&
        state.pendingIndicator == null &&
        state.importKeyAction == null &&
        state.watchIndicator == null
    ) return
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
        watchAddresses = watchAddresses,
        pendingIndicator = state.pendingIndicator,
        importKeyAction = state.importKeyAction,
        importKeyChainId = importKeyChainId,
    )
    putString(key, GsonHelper.customGson.toJson(cache))
}
