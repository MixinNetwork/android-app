package one.mixin.android.web3.details

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.NavBackStackEntry
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
    val token = if (knownToken?.assetId == assetId) {
        knownToken
    } else {
        web3ViewModel.web3TokenItemById(walletId, assetId)
    } ?: return
    if (!isAdded) return
    val address = web3ViewModel.getAddressesByChainId(walletId, token.chainId)?.destination
    if (!isAdded) return
    showWeb3AssetTransactions(token, address)
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
    if (nav.currentDestination?.id == R.id.web3_transactions_fragment &&
        nav.currentBackStackEntry?.tokenArgument()?.assetId == token.assetId
    ) {
        return true
    }
    val previous = nav.previousBackStackEntry
    if (previous?.destination?.id == R.id.web3_transactions_fragment &&
        previous.tokenArgument()?.assetId == token.assetId
    ) {
        activity?.onBackPressedDispatcher?.onBackPressed()
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
    return runCatching { nav.navigate(action, args) }.isSuccess
}

private fun NavBackStackEntry.tokenArgument(): Web3TokenItem? =
    arguments?.getParcelableCompat(Web3TransactionsFragment.ARGS_TOKEN, Web3TokenItem::class.java)
