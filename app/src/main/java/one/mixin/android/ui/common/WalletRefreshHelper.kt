package one.mixin.android.ui.common

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.util.reportException
import timber.log.Timber

object WalletRefreshHelper {

    fun startRefreshData(
        fragment: Fragment,
        web3ViewModel: Web3ViewModel,
        walletId: String,
        refreshJob: Job?,
        onWalletUpdated: (() -> Unit)? = null
    ): Job? {
        refreshJob?.cancel()
        return fragment.lifecycleScope.launch {
            refreshWalletData(
                web3ViewModel,
                walletId,
                onWalletUpdated
            )
        }
    }

    fun cancelRefreshData(refreshJob: Job?): Job? {
        refreshJob?.cancel()
        return null
    }

    private suspend fun refreshWalletData(
        web3ViewModel: Web3ViewModel,
        walletId: String,
        onWalletUpdated: (() -> Unit)? = null
    ) {
        try {
            while (true) {
                val wallet = web3ViewModel.findWalletById(walletId)
                if (wallet == null) {
                    Timber.w("Wallet not found: $walletId, stopping refresh")
                    break
                }

                try {
                    // Fetch wallet addresses
                    web3ViewModel.refreshWalletAddresses(walletId)

                    // Fetch wallet assets
                    web3ViewModel.refreshWalletAssets(walletId)

                    onWalletUpdated?.invoke()
                    Timber.d("Successfully refreshed wallet: $walletId")

                } catch (e: Exception) {
                    Timber.e(e, "Error refreshing wallet: $walletId")
                    reportException(e)
                }

                delay(30_000) // 30 seconds refresh interval
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in wallet refresh loop for wallet: $walletId")
        }
    }
}
