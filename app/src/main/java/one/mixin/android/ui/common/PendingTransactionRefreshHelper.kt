package one.mixin.android.ui.common

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.db.web3.vo.TransactionStatus
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshWeb3TransactionsJob
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.web3.js.JsSigner
import timber.log.Timber

object PendingTransactionRefreshHelper {
    
    fun startRefreshData(
        fragment: Fragment,
        web3ViewModel: Web3ViewModel,
        jobManager: MixinJobManager,
        refreshJob: Job?,
        onTransactionStatusUpdated: ((hash: String, newStatus: String) -> Unit)? = null
    ): Job? {
        refreshJob?.cancel()
        return fragment.lifecycleScope.launch {
            refreshTransactionData(
                JsSigner.currentWalletId,
                web3ViewModel,
                jobManager,
                onTransactionStatusUpdated
            )
        }
    }
    
    fun cancelRefreshData(refreshJob: Job?): Job? {
        refreshJob?.cancel()
        return null
    }
    
    private suspend fun refreshTransactionData(
        walletId: String,
        web3ViewModel: Web3ViewModel,
        jobManager: MixinJobManager,
        onTransactionStatusUpdated: ((hash: String, newStatus: String) -> Unit)? = null
    ) {
        try {
            while (true) {
                val pendingRawTransaction = web3ViewModel.getPendingRawTransactions(walletId)
                if (pendingRawTransaction.isEmpty()) {
                    val pendingTransaction = web3ViewModel.getPendingTransactions(walletId)
                    if (pendingTransaction.isNotEmpty()) {
                        jobManager.addJobInBackground(RefreshWeb3TransactionsJob())
                        delay(5_000)
                    } else {
                        delay(10_000)
                    }
                } else {
                    pendingRawTransaction.forEach { transition ->
                        val r = web3ViewModel.transaction(transition.hash, transition.chainId)
                        if (r.isSuccess && (r.data?.state == TransactionStatus.SUCCESS.value || 
                                           r.data?.state == TransactionStatus.FAILED.value || 
                                           r.isSuccess && r.data?.state == TransactionStatus.NOT_FOUND.value)) {
                            web3ViewModel.insertRawTransaction(r.data!!)
                            if (r.data?.state == TransactionStatus.FAILED.value || 
                               r.isSuccess && r.data?.state == TransactionStatus.NOT_FOUND.value || 
                               r.data?.state == TransactionStatus.SUCCESS.value) {
                                if (r.data?.state == TransactionStatus.SUCCESS.value) {
                                    jobManager.addJobInBackground(RefreshWeb3TransactionsJob())
                                }
                                if (r.data?.state != TransactionStatus.SUCCESS.value) {
                                    web3ViewModel.updateTransaction(transition.hash, r.data?.state!!, transition.chainId)
                                }
                                onTransactionStatusUpdated?.invoke(transition.hash, r.data?.state!!)
                            }
                        }
                    }
                    delay(5_000)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}
