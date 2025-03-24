package one.mixin.android.job

import androidx.collection.arraySetOf
import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.db.property.Web3PropertyHelper
import one.mixin.android.db.web3.vo.Web3Chain
import one.mixin.android.db.web3.vo.Web3Transaction
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import timber.log.Timber

class RefreshWeb3TransactionsJob(
) : BaseJob(Params(PRIORITY_UI_HIGH).requireNetwork().setGroupId(GROUP)) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshWeb3TransactionsJob"
        private const val KEY_LAST_CREATED_AT = "key_last_created_at"
        private const val DEFAULT_LIMIT = 30
    }

    override fun onRun(): Unit =
        runBlocking {
            try {
                val addresses = web3AddressDao.getAddress()
                if (addresses.isEmpty()) {
                    Timber.d("No addresses found to sync transactions")
                    return@runBlocking
                }

                Timber.d("Syncing transactions for ${addresses.size} addresses")
                addresses.distinctBy { it.destination }.forEach { address ->
                    val offset = getLastCreatedAt(address.destination)
                    fetchTransactions(address.destination, offset, DEFAULT_LIMIT)
                }

                Timber.d("Completed syncing transactions for all addresses")
            } catch (e: Exception) {
                Timber.e(e, "Error syncing all addresses transactions")
            }
        }

    private suspend fun fetchTransactions(destination: String, offset: String?, limit: Int): List<Web3Transaction>? {
        var result: List<Web3Transaction>? = null

        requestRouteAPI(
            invokeNetwork = {
                routeService.getAllTransactions(destination, offset, limit)
            },
            successBlock = { response ->
                result = response.data
                if (result.isNullOrEmpty()) {
                    Timber.d("No transactions returned from API for address $destination")
                } else {
                    web3TransactionDao.insertList(result!!)
                    Timber.d("Fetched ${result?.size} transactions from API for address $destination")
                }
                syncAsset(destination, result?.map { it.assetId }?.distinct())

                result?.lastOrNull()?.createdAt?.let {
                    saveLastCreatedAt(destination, it)
                    if ((result?.size ?: 0) >= DEFAULT_LIMIT) {
                        fetchTransactions(destination, it, limit)
                    }
                }
            },
            failureBlock = { response ->
                Timber.e("Failed to fetch transactions for address $destination: ${response.errorCode} - ${response.errorDescription}")
                false
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
            },
            defaultErrorHandle = {}
        )

        return result
    }

    private suspend fun saveLastCreatedAt(destination: String, timestamp: String) {
        Web3PropertyHelper.updateKeyValue(destination, timestamp)
    }

    private suspend fun getLastCreatedAt(destination: String): String? {
        return Web3PropertyHelper.findValueByKey(destination, null)
    }

    private suspend fun syncAsset(destination:String, ids: List<String>?) {
        if (ids.isNullOrEmpty()) return
        
        Timber.d("Syncing ${ids.size} assets")
        val chainId = arraySetOf<String>()
        ids.forEach { assetId ->
            try {
                val token = web3TokenDao.findTokenById(assetId)
                if (token == null) {
                    requestRouteAPI(
                        invokeNetwork = {
                            routeService.getAssetByAddress(assetId, destination)
                        },
                        successBlock = { response ->
                            val asset = response.data
                            if (asset != null) {
                                web3TokenDao.insert(asset)
                                chainId.add(asset.chainId)
                                Timber.d("Inserted ${asset.symbol} into database")
                            } else {
                                Timber.d("No asset found for wallet $assetId ${destination}")
                            }
                        },
                        failureBlock = { response ->
                            Timber.e("Failed to fetch asset for address ${destination} ${assetId}: ${response.errorCode} - ${response.errorDescription}")
                            false
                        },
                        requestSession = {
                            userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
                        },
                        defaultErrorHandle = {}
                    )
                } else {
                    Timber.d("Token $assetId already exists in local database, skipping fetch")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error syncing asset $assetId")
            }
        }
        fetchChain(chainId.toList())
    }

    private suspend fun fetchChain(chainIds: List<String>) {
        chainIds.forEach { chainId ->
            try {
                if (web3ChainDao.chainExists(chainId) == null) {
                    val response = tokenService.getChainById(chainId)
                    if (response.isSuccess) {
                        val chain = response.data
                        if (chain != null) {
                            web3ChainDao.insert(
                                Web3Chain(
                                    chainId = chain.chainId,
                                    name = chain.name,
                                    symbol = chain.symbol,
                                    iconUrl = chain.iconUrl,
                                    threshold = chain.threshold,
                                )
                            )
                            Timber.d("Successfully inserted ${chain.name} chain into database")
                        } else {
                            Timber.d("No chain found for chainId: $chainId")
                        }
                    } else {
                        Timber.e("Failed to fetch chain $chainId: ${response.errorCode} - ${response.errorDescription}")
                    }
                } else {
                    Timber.d("Chain $chainId already exists in local database, skipping fetch")
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception occurred while fetching chain $chainId")
            }
        }
    }
}
