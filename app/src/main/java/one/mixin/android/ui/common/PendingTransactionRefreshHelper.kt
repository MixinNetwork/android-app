package one.mixin.android.ui.common

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.db.web3.vo.TransactionStatus
import one.mixin.android.db.web3.vo.isGaslessSponsorPending
import one.mixin.android.db.web3.vo.isTerminalTransactionStatus
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshWeb3BitCoinJob
import one.mixin.android.job.RefreshWeb3TransactionsJob
import one.mixin.android.ui.home.web3.Web3ViewModel
import one.mixin.android.web3.js.Web3Signer
import one.mixin.android.api.response.web3.toPendingStatusOrNull
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
                Web3Signer.currentWalletId,
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
                if (pendingRawTransaction.isEmpty().not()) {
                    pendingRawTransaction.forEach { transition ->
                        if (transition.isGaslessSponsorPending()) {
                            val sponsorTransactionResponse = web3ViewModel.gaslessTransaction(transition.hash)
                            val sponsorTransaction = sponsorTransactionResponse.data
                            if (!sponsorTransactionResponse.isSuccess || sponsorTransaction == null) {
                                return@forEach
                            }
                            val broadcastTxHash = sponsorTransaction.broadcastTxHash?.takeIf { it.isNotBlank() }
                            if (broadcastTxHash != null) {
                                web3ViewModel.replaceGaslessPendingTransactionHash(
                                    walletId = walletId,
                                    sponsorTxId = transition.hash,
                                    broadcastTxHash = broadcastTxHash,
                                    chainId = transition.chainId,
                                    updatedAt = sponsorTransaction.updatedAt,
                                )
                                return@forEach
                            }
                            val gaslessStatus = sponsorTransaction.toPendingStatusOrNull()
                            if (gaslessStatus != null && gaslessStatus != TransactionStatus.PENDING.value) {
                                web3ViewModel.updateGaslessPendingTransactionStatus(
                                    walletId = walletId,
                                    hash = transition.hash,
                                    chainId = transition.chainId,
                                    status = gaslessStatus,
                                    updatedAt = sponsorTransaction.updatedAt,
                                )
                                onTransactionStatusUpdated?.invoke(transition.hash, gaslessStatus)
                            }
                            return@forEach
                        }
                        val r = web3ViewModel.transaction(transition.hash, transition.chainId)
                        if (r.isSuccess && r.data?.state.isTerminalTransactionStatus()) {
                            val rawTransaction = r.data ?: return@forEach
                            val rawToInsert =
                                if (rawTransaction.chainId == Constants.ChainId.BITCOIN_CHAIN_ID) {
                                    rawTransaction.copy(nonce = transition.nonce)
                                } else {
                                    rawTransaction
                                }
                            val btcRawTransactionHexToDeleteOutputs: String? =
                                if (transition.chainId == Constants.ChainId.BITCOIN_CHAIN_ID && rawTransaction.state == TransactionStatus.NOT_FOUND.value) {
                                    transition.raw
                                } else {
                                    null
                                }
                            web3ViewModel.insertRawTransactionAndUpdateTransactionStatus(
                                raw = rawToInsert,
                                hash = transition.hash,
                                chainId = transition.chainId,
                                status = rawTransaction.state,
                                btcRawTransactionHexToDeleteOutputs = btcRawTransactionHexToDeleteOutputs,
                            )
                            if (r.data?.state.isTerminalTransactionStatus()) {
                                if (r.data?.state == TransactionStatus.SUCCESS.value) {
                                    jobManager.addJobInBackground(RefreshWeb3TransactionsJob(walletId))
                                }
                                if (transition.chainId == Constants.ChainId.BITCOIN_CHAIN_ID && r.data?.state == TransactionStatus.NOT_FOUND.value) {
                                    jobManager.addJobInBackground(RefreshWeb3BitCoinJob(walletId))
                                }
                                onTransactionStatusUpdated?.invoke(transition.hash, r.data?.state!!)
                            }
                        }
                    }
                    delay(5_000)
                } else {
                    delay(15_000)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}
