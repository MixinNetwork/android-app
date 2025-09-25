package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.RxBus
import one.mixin.android.db.web3.vo.Web3Chain
import one.mixin.android.db.web3.vo.Web3TokensExtra
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.event.WalletOperationType
import one.mixin.android.event.WalletRefreshedEvent
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import timber.log.Timber

class RefreshSingleWalletJob(
    private val walletId: String,
) : BaseJob(
    Params(PRIORITY_UI_HIGH).singleInstanceBy(GROUP + walletId).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshSingleWalletJob"
    }

    override fun onRun(): Unit = runBlocking {
        try {
            val wallet = web3WalletDao.getWalletById(walletId)
            if (wallet == null) {
                return@runBlocking
            }
            fetchWalletAddresses(wallet)
            fetchWalletAssets(wallet)
            RxBus.publish(WalletRefreshedEvent(walletId, WalletOperationType.OTHER))
            Timber.e("Successfully refreshed wallet: $walletId")
            jobManager.addJobInBackground(RefreshWeb3TransactionsJob(walletId))
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh wallet: $walletId")
        }
    }

    private suspend fun fetchWalletAddresses(wallet: Web3Wallet) {
        requestRouteAPI(
            invokeNetwork = {
                routeService.getWalletAddresses(wallet.id)
            },
            successBlock = { response ->
                val addressesResponse = response
                if (addressesResponse.data.isNullOrEmpty().not()) {
                    Timber.d("Fetched ${addressesResponse.data?.size} addresses for wallet ${wallet.id}")
                    val web3Addresses = addressesResponse.data!!
                    web3AddressDao.insertList(web3Addresses)
                    Timber.d("Inserted ${web3Addresses.size} addresses into database")
                } else {
                    Timber.d("No addresses found for wallet ${wallet.id}")
                }
            },
            failureBlock = { response ->
                Timber.e("Failed to fetch addresses for wallet ${wallet.id}: ${response.errorCode} - ${response.errorDescription}")
                false
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
            },
            defaultErrorHandle = {}
        )
    }

    private suspend fun fetchWalletAssets(wallet: Web3Wallet) {
        requestRouteAPI(
            invokeNetwork = {
                routeService.getWalletAssets(wallet.id)
            },
            successBlock = { response ->
                val assets = response.data
                if (assets != null && assets.isNotEmpty()) {
                    Timber.d("Fetched ${assets.size} assets for wallet ${wallet.id}")
                    val assetIds = assets.map { it.assetId }
                    web3TokenDao.updateBalanceToZeroForMissingAssets(wallet.id, assetIds)
                    Timber.d("Updated missing assets to zero balance for wallet ${wallet.id}")
                    val extrasToInsert = assets.filter { it.level < Constants.AssetLevel.UNKNOWN }
                        .mapNotNull { asset ->
                            val extra = web3TokensExtraDao.findByAssetId(asset.assetId, wallet.id)
                            if (extra == null) {
                                Web3TokensExtra(
                                    assetId = asset.assetId,
                                    walletId = wallet.id,
                                    hidden = true
                                )
                            } else {
                                null
                            }
                        }
                    if (extrasToInsert.isNotEmpty()) {
                        web3TokensExtraDao.insertList(extrasToInsert)
                    }
                    web3TokenDao.insertList(assets)
                    fetchChain(assets.map { it.chainId }.distinct())
                    Timber.d("Inserted ${assets.size} tokens into database")
                } else {
                    Timber.d("No assets found for wallet ${wallet.id}")
                    web3TokenDao.updateAllBalancesToZero(wallet.id)
                    Timber.d("Updated all assets to zero balance for wallet ${wallet.id}")
                }
            },
            failureBlock = { response ->
                Timber.e("Failed to fetch assets for wallet ${wallet.id}: ${response.errorCode} - ${response.errorDescription}")
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
