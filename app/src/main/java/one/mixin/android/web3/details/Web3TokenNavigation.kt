package one.mixin.android.web3.details

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import one.mixin.android.R
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.ui.wallet.WalletActivity

internal suspend fun Fragment.openWeb3AssetTransactions(
    web3ViewModel: Web3ViewModel,
    walletId: String,
    assetId: String,
    knownToken: Web3TokenItem? = null,
) {
    if (!isAdded || assetId.isBlank()) return
    val walletToken = if (knownToken?.assetId == assetId && knownToken.walletId == walletId) {
        null
    } else {
        web3ViewModel.web3TokenItemById(walletId, assetId)
    }
    val token = tokenForTransactionWallet(walletId, assetId, knownToken, walletToken) ?: return
    if (!isAdded) return
    val address = web3ViewModel.getAddressesByChainId(walletId, token.chainId)?.destination
    if (!isAdded) return
    showWeb3AssetTransactions(token, address)
}

internal fun tokenForTransactionWallet(
    walletId: String,
    assetId: String,
    knownToken: Web3TokenItem?,
    walletToken: Web3TokenItem?,
): Web3TokenItem? {
    if (assetId.isBlank()) return null
    if (knownToken?.assetId == assetId && knownToken.walletId == walletId) return knownToken
    walletToken?.let { return it }
    return knownToken
        ?.takeIf { it.assetId == assetId }
        ?.copy(walletId = walletId, balance = "0", hidden = null)
}

internal fun Fragment.showWeb3AssetTransactions(
    token: Web3TokenItem,
    address: String?,
) {
    val host = activity ?: return
    if (host is WalletActivity && navigateToWeb3Asset(token, address)) return
    WalletActivity.showWithWeb3Token(
        host,
        token,
        address,
        WalletActivity.Destination.Web3Transactions,
    )
}

private fun Fragment.navigateToWeb3Asset(
    token: Web3TokenItem,
    address: String?,
): Boolean {
    val nav = runCatching { findNavController() }.getOrNull() ?: return false
    val existingToken = nav.findBackStackEntry(R.id.web3_transactions_fragment)?.tokenArgument()
    if (existingToken?.assetId == token.assetId && existingToken.walletId == token.walletId) {
        nav.popBackStack(R.id.web3_transactions_fragment, false)
        return true
    }
    val action = when (nav.currentDestination?.id) {
        R.id.web3_transaction_fragment -> R.id.action_web3_transaction_to_web3_transactions
        R.id.all_web3_transactions_fragment -> R.id.action_all_web3_transactions_fragment_to_web3_transactions
        R.id.web3_transactions_fragment -> R.id.action_web3_transactions_to_web3_transactions_token
        else -> return false
    }
    val args = Bundle().apply {
        putParcelable(Web3TransactionsFragment.ARGS_TOKEN, token)
        if (!address.isNullOrEmpty()) putString(Web3TransactionsFragment.ARGS_ADDRESS, address)
    }
    return runCatching { nav.navigateReplacingExisting(action, args, R.id.web3_transactions_fragment) }.isSuccess
}

internal fun NavController.navigateToWeb3Transaction(
    @IdRes resId: Int,
    args: Bundle,
) {
    navigateReplacingExisting(resId, args, R.id.web3_transaction_fragment)
}

// Keep at most one entry of destinationId in the back stack: pop the existing one (and everything above it) before navigating.
private fun NavController.navigateReplacingExisting(
    @IdRes resId: Int,
    args: Bundle,
    @IdRes destinationId: Int,
) {
    if (findBackStackEntry(destinationId) == null) {
        navigate(resId, args)
        return
    }
    val actionOptions = currentDestination?.getAction(resId)?.navOptions
    val options = NavOptions.Builder()
        .setEnterAnim(actionOptions?.enterAnim ?: -1)
        .setExitAnim(actionOptions?.exitAnim ?: -1)
        .setPopEnterAnim(actionOptions?.popEnterAnim ?: -1)
        .setPopExitAnim(actionOptions?.popExitAnim ?: -1)
        .setPopUpTo(destinationId, true)
        .build()
    navigate(resId, args, options)
}

private fun NavController.findBackStackEntry(@IdRes destinationId: Int): NavBackStackEntry? =
    runCatching { getBackStackEntry(destinationId) }.getOrNull()

private fun NavBackStackEntry.tokenArgument(): Web3TokenItem? =
    arguments?.getParcelableCompat(Web3TransactionsFragment.ARGS_TOKEN, Web3TokenItem::class.java)
