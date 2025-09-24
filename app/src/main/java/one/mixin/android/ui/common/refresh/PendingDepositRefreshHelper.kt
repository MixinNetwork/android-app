package one.mixin.android.ui.common.refresh

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.util.reportException
import one.mixin.android.vo.safe.toSnapshot
import timber.log.Timber

object PendingDepositRefreshHelper {

    fun startRefreshData(
        fragment: Fragment,
        walletViewModel: WalletViewModel,
        refreshJob: Job?,
        onPendingDepositUpdated: (() -> Unit)? = null
    ): Job? {
        refreshJob?.cancel()
        return fragment.lifecycleScope.launch(Dispatchers.IO)  {
            refreshPendingDepositData(
                walletViewModel,
                onPendingDepositUpdated
            )
        }
    }

    fun cancelRefreshData(refreshJob: Job?): Job? {
        refreshJob?.cancel()
        return null
    }

    private suspend fun refreshPendingDepositData(
        walletViewModel: WalletViewModel,
        onPendingDepositUpdated: (() -> Unit)? = null
    ) {
        try {
            while (true) {
                handleMixinResponse(
                    invokeNetwork = { walletViewModel.allPendingDeposit() },
                    exceptionBlock = { e ->
                        reportException(e)
                        false
                    },
                    successBlock = {
                        val pendingDeposits = it.data ?: emptyList()
                        val destinationTags = walletViewModel.findDepositEntryDestinations()
                        pendingDeposits
                            .filter { pd ->
                                destinationTags.any { dt ->
                                    dt.destination == pd.destination && (dt.tag.isNullOrBlank() || dt.tag == pd.tag)
                                }
                            }
                            .map { pd -> pd.toSnapshot() }.let { snapshots ->
                                // If there are no pending deposit snapshots belonging to the current user, clear all pending deposits
                                if (snapshots.isEmpty()) {
                                    walletViewModel.clearAllPendingDeposits()
                                    return@let
                                }
                                snapshots.map { it.assetId }.distinct().forEach {
                                    walletViewModel.findOrSyncAsset(it)
                                }
                                walletViewModel.insertPendingDeposit(snapshots)
                                onPendingDepositUpdated?.invoke()
                            }
                    },
                )

                delay(10_000)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error refreshing pending deposits")
        }
    }
}
