package one.mixin.android.ui.common.refresh

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
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
        walletId: String?,
        refreshJob: Job?,
        onWalletUpdated: (() -> Unit)? = null
    ): Job? {
        refreshJob?.cancel()
        return fragment.lifecycleScope.launch(Dispatchers.IO) {
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
        walletId: String?,
        onWalletUpdated: (() -> Unit)? = null
    ) {
        try {
            while (true) {
                if (walletId == null) {
                    val allWallets = web3ViewModel.getAllWallets()
                    if (allWallets.isEmpty()) {
                        Timber.w("No wallets found, stopping refresh")
                        break
                    }

                    Timber.d("Found ${allWallets.size} wallets to refresh")

                    for (wallet in allWallets) {
                        try {
                            web3ViewModel.refreshWalletAddresses(wallet.id)

                            web3ViewModel.refreshWalletAssets(wallet.id)

                            Timber.d("Successfully refreshed wallet: ${wallet.id}")
                        } catch (e: Exception) {
                            Timber.e(e, "Error refreshing wallet: ${wallet.id}")
                            reportException(e)
                        }
                    }

                    onWalletUpdated?.invoke()
                    Timber.d("Successfully refreshed all ${allWallets.size} wallets")
                } else {
                    val wallet = web3ViewModel.findWalletById(walletId)
                    if (wallet == null) {
                        Timber.w("Wallet not found: $walletId, stopping refresh")
                        break
                    }

                    try {
                        web3ViewModel.refreshWalletAddresses(walletId)

                        web3ViewModel.refreshWalletAssets(walletId)

                        onWalletUpdated?.invoke()
                        Timber.d("Successfully refreshed wallet: $walletId")

                    } catch (e: Exception) {
                        Timber.e(e, "Error refreshing wallet: $walletId")
                        reportException(e)
                    }
                }

                delay(10_000) // 10 seconds refresh interval
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in wallet refresh loop for walletId: $walletId")
        }
    }
}