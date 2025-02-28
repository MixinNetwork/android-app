package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.SOLANA_ADDRESS
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.api.request.web3.AddressRequest
import one.mixin.android.api.request.web3.WalletRequest
import one.mixin.android.api.response.Web3Address
import one.mixin.android.api.response.Web3Wallet
import one.mixin.android.api.response.WalletAddress
import one.mixin.android.api.response.WalletAddressResponse
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import timber.log.Timber

class RefreshWeb3Job : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).persist().requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshWeb3Job"

        const val WALLET_CATEGORY_CLASSIC = "classic"
        const val WALLET_CATEGORY_PRIVATE = "private"
    }

    override fun onRun(): Unit = runBlocking {
        val wallets = web3WalletDao.getAllWallets()
        if (wallets.isEmpty()) {
            val erc20Address = PropertyHelper.findValueByKey(EVM_ADDRESS, "")
            val solAddress = PropertyHelper.findValueByKey(SOLANA_ADDRESS, "")
            createWallet(
                WALLET_CATEGORY_CLASSIC, listOf(
                    AddressRequest(
                        destination = erc20Address,
                        tag = "",
                        chainId = Constants.ChainId.ETHEREUM_CHAIN_ID
                    ),
                    AddressRequest(
                        destination = solAddress,
                        tag = "",
                        chainId = Constants.ChainId.SOLANA_CHAIN_ID
                    )
                )
            )
        } else {
            wallets.forEach { wallet ->
                fetchWalletAssets(wallet)
            }
        }
    }
    
    private suspend fun createWallet(category: String, addresses: List<AddressRequest>) {
        val walletRequest = WalletRequest(
            name = category,
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
                    web3WalletDao.insert(wallet)
                    Timber.d("Created $category wallet with ID: ${wallet.id}")

                    fetchWalletAddresses(wallet)
                    fetchWalletAssets(wallet)
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
            }
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
            }
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
                    if (assets.isNotEmpty()) {
                        web3TokenDao.insertList(assets)
                        Timber.d("Inserted ${assets.size} tokens into database")
                    }
                } else {
                    Timber.d("No assets found for wallet ${wallet.id}")
                }
            },
            failureBlock = { response ->
                Timber.e("Failed to fetch assets for wallet ${wallet.id}: ${response.errorCode} - ${response.errorDescription}")
                false
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
            }
        )
    }
}