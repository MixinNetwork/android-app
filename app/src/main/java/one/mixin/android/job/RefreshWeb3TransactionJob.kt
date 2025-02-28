package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.db.web3.vo.Web3Transaction
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import timber.log.Timber

class RefreshWeb3TransactionJob(
) : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).persist().requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshWeb3TransactionJob"
        
        private const val DEFAULT_LIMIT = 100
    }

    override fun onRun(): Unit =
        runBlocking {
            syncTransactions()
        }
    
    private suspend fun syncTransactions() {
        try {
            var hasMoreData = true
            var offset: String? = null
            var totalSynced = 0
            
            val latestTransaction = web3TransactionDao.getLatestTransaction()
            if (latestTransaction != null) {
                offset = latestTransaction.createdAt
                Timber.d("Starting transaction sync from offset: $offset")
            } else {
                Timber.d("No local transactions found, syncing from beginning")
            }
            
            while (hasMoreData) {
                val transactions = fetchTransactions(offset, DEFAULT_LIMIT)
                
                if (transactions.isNullOrEmpty()) {
                    hasMoreData = false
                    Timber.d("No more transactions to sync")
                } else {
                    web3TransactionDao.insertList(transactions)
                    
                    totalSynced += transactions.size
                    Timber.d("Synced ${transactions.size} transactions, total: $totalSynced")
                    
                    if (transactions.size < DEFAULT_LIMIT) {
                        hasMoreData = false
                        Timber.d("Reached end of transactions")
                    } else {
                        offset = transactions.last().createdAt
                        Timber.d("Updated offset to: $offset")
                    }
                }
            }
            
            Timber.d("Transaction sync completed, total synced: $totalSynced")
        } catch (e: Exception) {
            Timber.e(e, "Error syncing transactions")
        }
    }
    

    private suspend fun fetchTransactions(offset: String?, limit: Int): List<Web3Transaction>? {
        var result: List<Web3Transaction>? = null
        
        requestRouteAPI(
            invokeNetwork = {
                routeService.getAllTransactions(offset, limit)
            },
            successBlock = { response ->
                result = response.data
                if (result.isNullOrEmpty()) {
                    Timber.d("No transactions returned from API")
                } else {
                    Timber.d("Fetched ${result?.size} transactions from API")
                }
            },
            failureBlock = { response ->
                Timber.e("Failed to fetch transactions: ${response.errorCode} - ${response.errorDescription}")
                false
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
            }
        )
        
        return result
    }
}
