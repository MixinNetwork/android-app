package one.mixin.android.ui.address

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.db.web3.vo.Web3TransactionItem
import one.mixin.android.db.web3.vo.toWeb3Wallet
import one.mixin.android.extension.toast
import one.mixin.android.ui.wallet.TransactionFragment
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.SnapshotItem
import one.mixin.android.vo.safe.toWeb3TokenItem
import one.mixin.android.web3.details.Web3TransactionFragment
import one.mixin.android.web3.details.Web3TransactionsFragment

fun Fragment.openAddressTransaction(transaction: SnapshotItem, viewModel: AddressViewModel) {
    openAddressTransaction(R.id.transaction_fragment) {
        viewModel.findOrSyncAsset(transaction.assetId)?.let { asset ->
            Bundle().apply {
                putParcelable(TransactionFragment.ARGS_SNAPSHOT, transaction)
                putParcelable(TransactionsFragment.ARGS_ASSET, asset)
            }
        }
    }
}

fun Fragment.openAddressTransaction(transaction: Web3TransactionItem, walletId: String, viewModel: AddressViewModel) {
    openAddressTransaction(R.id.web3_transaction_fragment) {
        val wallet = viewModel.findWeb3WalletById(walletId)
        if (wallet == null) {
            null
        } else {
            val assetId = transaction.getMainAssetId()
            val token = viewModel.web3TokenItemById(walletId, assetId)
                ?: viewModel.findOrSyncAsset(assetId)?.let { asset ->
                    asset.toWeb3TokenItem(walletId).copy(balance = "0", precision = asset.precision)
                }
            token?.let {
                Bundle().apply {
                    putParcelable(Web3TransactionFragment.ARGS_TRANSACTION, transaction)
                    putParcelable(Web3TransactionsFragment.ARGS_TOKEN, it)
                    putString(Web3TransactionFragment.ARGS_CHAIN, transaction.chainId)
                    putParcelable(Web3TransactionFragment.ARGS_WALLET, wallet.toWeb3Wallet())
                }
            }
        }
    }
}

private fun Fragment.openAddressTransaction(@IdRes destinationId: Int, loadArguments: suspend () -> Bundle?) {
    val owner = viewLifecycleOwner
    val navController = findNavController()
    val entry = navController.currentBackStackEntry
    owner.lifecycleScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, error -> ErrorHandler.handleError(error) }) {
        val args = loadArguments()
        withContext(Dispatchers.Main) {
            if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || navController.currentBackStackEntry != entry) return@withContext
            if (args == null) {
                toast(R.string.Data_error)
                return@withContext
            }
            navController.navigate(destinationId, args)
        }
    }
}
