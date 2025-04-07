package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.SOLANA_ADDRESS
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.request.web3.Web3AddressRequest
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.db.web3.vo.Web3Chain
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import timber.log.Timber

class RefreshWeb3Job : BaseJob(
    Params(PRIORITY_UI_HIGH).singleInstanceBy(GROUP).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshWeb3Job"

        const val WALLET_CATEGORY_CLASSIC = "classic"
        const val WALLET_CATEGORY_PRIVATE = "private"
    }

    override fun onRun(): Unit = runBlocking {
        fetchWallets()
        val wallets = web3WalletDao.getAllWallets()
        if (wallets.isEmpty()) {
            val erc20Address = PropertyHelper.findValueByKey(EVM_ADDRESS, "")
            val solAddress = PropertyHelper.findValueByKey(SOLANA_ADDRESS, "")
            createWallet(
                "ClassicWallet", WALLET_CATEGORY_CLASSIC, listOf(
                    Web3AddressRequest(
                        destination = erc20Address,
                        chainId = Constants.ChainId.Polygon
                    ),
                    Web3AddressRequest(
                        destination = solAddress,
                        chainId = Constants.ChainId.SOLANA_CHAIN_ID
                    )
                )
            )
        } else {
            fetchChain()
            wallets.forEach { wallet ->
                fetchWalletAssets(wallet)
            }
        }
        jobManager.addJobInBackground(RefreshWeb3TransactionsJob())
    }

    private suspend fun createWallet(name: String, category: String, addresses: List<Web3AddressRequest>) {
        val walletRequest = WalletRequest(
            name = name,
            category = category,
            addresses = addresses
        )

        requestRouteAPI(
            invokeNetwork = {
                routeService.createWallet(walletRequest)
            },
            successBlock = { response ->
                val wallet = response.data
                if (wallet != null) {
                    val w = Web3Wallet(wallet.id, wallet.category, wallet.name, wallet.createdAt, wallet.updatedAt)
                    web3WalletDao.insert(w)
                    Timber.d("Created ${wallet.category} wallet with ID: ${wallet.id}")
                    wallet.addresses?.let {
                        web3AddressDao.insertList(it)
                        Timber.d("Inserted wallet with ID: ${wallet.id}, ${addresses.size} addresses")
                    }
                    fetchChain()
                    fetchWalletAssets(w)
                } else {
                    Timber.e("Failed to create $category wallet: response data is null")
                }
            },
            failureBlock = { response ->
                Timber.e("Failed to create $category wallet: ${response.errorCode} - ${response.errorDescription}")
                false
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
            },
            defaultErrorHandle = {}
        )
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

    private suspend fun fetchWallets() {
        requestRouteAPI(
            invokeNetwork = {
                routeService.getWallets()
            },
            successBlock = { response ->
                val wallets = response.data
                wallets?.let {
                    web3WalletDao.insertList(it)
                    wallets.forEach { wallet ->
                        fetchWalletAddresses(wallet)
                    }
                }
            },
            failureBlock = { response ->
                Timber.e("Failed to fetch wallets ${response.errorCode} - ${response.errorDescription}")
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

    private suspend fun fetchChain() {
        try {
            val response = tokenService.getChains()
            if (response.isSuccess) {
                val chains = response.data
                if (chains != null && chains.isNotEmpty()) {
                    Timber.d("Fetched ${chains.size} chains")
                    val web3Chains = chains.map { chain ->
                        Web3Chain(
                            chainId = chain.chainId,
                            name = chain.name,
                            symbol = chain.symbol,
                            iconUrl = chain.iconUrl,
                            threshold = chain.threshold,
                        )
                    }
                    web3ChainDao.insertList(web3Chains)
                    Timber.d("Successfully inserted ${web3Chains.size} chains into database")
                } else {
                    Timber.d("No chains found")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception occurred while fetching chains")
        }
    }
}