package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.db.web3.vo.Web3Chain
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import timber.log.Timber

class RefreshWeb3TokenJob(
    private val walletId: String?,
    private val assetId: String? = null,
    private val address:String? = null
) : BaseJob(Params(PRIORITY_UI_HIGH).requireNetwork().setGroupId(GROUP)) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshWeb3TokenJob"
    }

    override fun onRun(): Unit =
        runBlocking {
            try {
                if (walletId != null) {
                    fetchWalletAssets(walletId)
                } else if (assetId != null && address!=null){
                    fetchAsset(assetId, address)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

    private suspend fun fetchWalletAssets(walletId: String) {
        requestRouteAPI(
            invokeNetwork = {
                routeService.getWalletAssets(walletId)
            },
            successBlock = { response ->
                val assets = response.data
                if (assets != null && assets.isNotEmpty()) {
                    Timber.d("Fetched ${assets.size} assets for wallet ${walletId}")
                    if (assets.isNotEmpty()) {
                        web3TokenDao.insertList(assets)
                        fetchChain(assets.map { it.chainId }.distinct())
                        Timber.d("Inserted ${assets.size} tokens into database")
                    }
                } else {
                    Timber.d("No assets found for wallet ${walletId}")
                }
            },
            failureBlock = { response ->
                Timber.e("Failed to fetch assets for wallet ${walletId}: ${response.errorCode} - ${response.errorDescription}")
                false
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
            },
            defaultErrorHandle = {}
        )
    }

    private suspend fun fetchAsset(assetId: String, address: String) {
        requestRouteAPI(
            invokeNetwork = {
                routeService.getAssetByAddress(assetId, address)
            },
            successBlock = { response ->
                val asset = response.data
                if (asset != null) {
                    Timber.d("Fetched ${asset.symbol} assets for address ${address}")
                    web3TokenDao.insert(asset)
                    fetchChain(listOf(asset.chainId))
                    Timber.d("Inserted ${asset.symbol} into database")
                } else {
                    Timber.d("No asset found for wallet $assetId ${address}")
                }
            },
            failureBlock = { response ->
                Timber.e("Failed to fetch asset for address ${address} ${assetId}: ${response.errorCode} - ${response.errorDescription}")
                false
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
            },
            defaultErrorHandle = {}
        )
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
