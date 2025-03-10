package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3Transaction
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import timber.log.Timber

class RefreshWeb3TransactionJob(
) : BaseJob(Params(PRIORITY_UI_HIGH).requireNetwork().setGroupId(GROUP)) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshWeb3TransactionJob"
        private const val KEY_LAST_CREATED_AT = "key_last_created_at"

        private const val DEFAULT_LIMIT = 30
    }

    override fun onRun(): Unit =
        runBlocking {
            try {
                val addresses = web3AddressDao.getAddressIds()
                if (addresses.isEmpty()) {
                    Timber.d("No addresses found to sync transactions")
                    return@runBlocking
                }

                Timber.d("Syncing transactions for ${addresses.size} addresses")
                addresses.forEach { address ->
                    val offset = getLastCreatedAt(address)
                    fetchTransactions(address, offset, DEFAULT_LIMIT)
                }

                Timber.d("Completed syncing transactions for all addresses")
            } catch (e: Exception) {
                Timber.e(e, "Error syncing all addresses transactions")
            }
        }

    private suspend fun fetchTransactions(addressId: String, offset: String?, limit: Int): List<Web3Transaction>? {
        var result: List<Web3Transaction>? = null

        requestRouteAPI(
            invokeNetwork = {
                routeService.getAllTransactions(addressId, offset, limit)
            },
            successBlock = { response ->
                result = response.data
                if (result.isNullOrEmpty()) {
                    Timber.d("No transactions returned from API for address $addressId")
                } else {
                    web3TransactionDao.insertList(result!!)
                    Timber.d("Fetched ${result?.size} transactions from API for address $addressId")
                }
                if ((result?.size ?: 0) >= DEFAULT_LIMIT) {
                    result?.lastOrNull()?.createdAt?.let {
                        saveLastCreatedAt(addressId, it)
                        fetchTransactions(addressId, it, limit)
                    }
                }
            },
            failureBlock = { response ->
                Timber.e("Failed to fetch transactions for address $addressId: ${response.errorCode} - ${response.errorDescription}")
                false
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
            },
            defaultErrorHandle = {}
        )

        return result
    }

    private suspend fun saveLastCreatedAt(addressId: String, timestamp: String) {
        PropertyHelper.updateKeyValue(addressId, timestamp)
    }

    private suspend fun getLastCreatedAt(addressId: String): String? {
        return PropertyHelper.findValueByKey(addressId, null)
    }
}
