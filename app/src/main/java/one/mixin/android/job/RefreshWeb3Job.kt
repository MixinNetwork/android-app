package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.SOLANA_ADDRESS
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.request.web3.Web3AddressRequest
import one.mixin.android.api.response.web3.WalletOutput
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.db.web3.vo.Web3Chain
import one.mixin.android.db.web3.vo.Web3TokensExtra
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.event.WalletRefreshedEvent
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import one.mixin.android.vo.WalletCategory
import one.mixin.android.R
import one.mixin.android.event.WalletOperationType
import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.web3.js.Web3Signer
import timber.log.Timber
import kotlin.collections.isNullOrEmpty
import kotlin.collections.take

class RefreshWeb3Job : BaseJob(
    Params(PRIORITY_UI_HIGH).singleInstanceBy(GROUP).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshWeb3Job"
    }

    override fun onRun(): Unit = runBlocking {
        fetchWallets()
        val wallets = web3WalletDao.getAllClassicWallets()
        if (wallets.isEmpty()) {
            val erc20Address = PropertyHelper.findValueByKey(EVM_ADDRESS, "")
            val solAddress = PropertyHelper.findValueByKey(SOLANA_ADDRESS, "")
            if (erc20Address.isBlank() || solAddress.isBlank()) {
                Timber.e("EVM or Solana address is not set")
                return@runBlocking
            }
            createWallet(
                applicationContext.getString(R.string.Common_Wallet), WalletCategory.CLASSIC.value, listOf(
                    Web3AddressRequest(
                        destination = erc20Address,
                        chainId = Constants.ChainId.ETHEREUM_CHAIN_ID,
                        path = Bip44Path.ethereumPathString()
                    ),
                    Web3AddressRequest(
                        destination = solAddress,
                        chainId = Constants.ChainId.SOLANA_CHAIN_ID,
                        path = Bip44Path.solanaPathString()
                    )
                )
            )
        } else {
            wallets.forEach { wallet ->
                if (web3AddressDao.getAddressesByWalletId(wallet.id).any {
                        it.path == null || it.path.isBlank()
                    }) {
                    try {
                        routeService.updateWallet(wallet.id, WalletRequest(name = MixinApplication.appContext.getString(R.string.Common_Wallet), null, null))
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to rename wallet ${wallet.id}")
                    }
                    fetchWallets(wallet.id)
                }
            }
        }
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
                    fetchWalletAssets(w.id)
                    Web3Signer.init(
                        { w.id },
                        { walletId ->
                            runBlocking(Dispatchers.IO) { web3AddressDao.getAddressesByWalletId(walletId) }
                        }, { walletId ->
                            runBlocking(Dispatchers.IO) { web3WalletDao.getWalletById(walletId) }
                        }
                    )
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

    private suspend fun fetchWalletAddresses(walletId: String) {
        requestRouteAPI(
            invokeNetwork = {
                routeService.getWalletAddresses(walletId)
            },
            successBlock = { response ->
                val addressesResponse = response
                if (addressesResponse.data.isNullOrEmpty().not()) {
                    Timber.d("Fetched ${addressesResponse.data?.size} addresses for wallet $walletId")
                    val web3Addresses = addressesResponse.data!!
                    web3AddressDao.insertList(web3Addresses)
                    Timber.d("Inserted ${web3Addresses.size} addresses into database")
                    val btcAddress = web3Addresses.firstOrNull { it.chainId == Constants.ChainId.BITCOIN_CHAIN_ID }?.destination
                    if (btcAddress.isNullOrBlank().not()) {
                        fetchBtcOutputs(walletId = walletId, address = btcAddress)
                    }
                } else {
                    Timber.d("No addresses found for wallet $walletId")
                }
            },
            failureBlock = { response ->
                Timber.e("Failed to fetch addresses for wallet $walletId: ${response.errorCode} - ${response.errorDescription}")
                false
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
            },
            defaultErrorHandle = {}
        )
    }

    private suspend fun fetchBtcOutputs(walletId: String, address: String) {
        requestRouteAPI(
            invokeNetwork = {
                routeService.getWalletOutputs(walletId = walletId, address = address)
            },
            successBlock = { response ->
                val outputs = response.data
                try {
                    // use suspend insert to let Room handle the list insertion in coroutine
                    val safeOutputs: List<WalletOutput> = outputs ?: emptyList()
                    walletOutputDao.mergeOutputsForAddress(address, safeOutputs)
                    Timber.d("Merged ${safeOutputs.size} BTC outputs into database for walletId=$walletId")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to insert BTC outputs for walletId=$walletId into DB")
                }
            },
            failureBlock = { response ->
                Timber.e("Failed to fetch BTC outputs for walletId=$walletId address=$address: ${response.errorCode} - ${response.errorDescription}")
                false
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
            },
            defaultErrorHandle = {}
        )
    }

    private suspend fun fetchWallets(renameWalletId: String? = null) {
        requestRouteAPI(
            invokeNetwork = {
                routeService.getWallets()
            },
            successBlock = { response ->
                val wallets = response.data
                wallets?.let {
                    web3WalletDao.insertList(it)
                    wallets.forEach { wallet ->
                        if (wallet.id == renameWalletId) {
                            RxBus.publish(WalletRefreshedEvent(wallet.id, WalletOperationType.RENAME))
                        }
                        fetchWalletAddresses(wallet.id)
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

    private suspend fun fetchWalletAssets(walletId: String) {
        requestRouteAPI(
            invokeNetwork = {
                routeService.getWalletAssets(walletId)
            },
            successBlock = { response ->
                val assets = response.data
                if (assets != null && assets.isNotEmpty()) {
                    Timber.d("Fetched ${assets.size} assets for wallet $walletId")
                    val assetIds = assets.map { it.assetId }
                    web3TokenDao.updateBalanceToZeroForMissingAssets(walletId, assetIds)
                    Timber.d("Updated missing assets to zero balance for wallet $walletId")
                    val extrasToInsert = assets.filter { it.level < Constants.AssetLevel.UNKNOWN }
                        .mapNotNull { asset ->
                            val extra = web3TokensExtraDao.findByAssetId(asset.assetId, walletId)
                            if (extra == null) {
                                Web3TokensExtra(
                                    assetId = asset.assetId,
                                    walletId = walletId,
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
                    Timber.d("No assets found for wallet $walletId")
                    web3TokenDao.updateAllBalancesToZero(walletId)
                    Timber.d("Updated all assets to zero balance for wallet $walletId")
                }
            },
            failureBlock = { response ->
                Timber.e("Failed to fetch assets for wallet $walletId: ${response.errorCode} - ${response.errorDescription}")
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