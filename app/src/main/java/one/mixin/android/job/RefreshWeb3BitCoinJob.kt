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
import java.math.BigDecimal
import kotlin.collections.isNullOrEmpty
import kotlin.collections.take

class RefreshWeb3BitCoinJob(val walletId: String) : BaseJob(
    Params(PRIORITY_UI_HIGH).singleInstanceBy(GROUP).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshWeb3BitCoinJob"
    }

    override fun onRun(): Unit = runBlocking {
        val address = web3AddressDao.getAddressesByChainId(walletId, Constants.ChainId.BITCOIN_CHAIN_ID)?:return@runBlocking
        fetchBtcOutputs(walletId, address.destination)
    }

    private suspend fun fetchBtcOutputs(walletId: String, address: String) {
        requestRouteAPI(
            invokeNetwork = {
                routeService.getWalletOutputs(walletId = walletId, address = address, assetId = Constants.ChainId.BITCOIN_CHAIN_ID)
            },
            successBlock = { response ->
                val outputs = response.data
                try {
                    // use suspend insert to let Room handle the list insertion in coroutine
                    val safeOutputs: List<WalletOutput> = outputs ?: emptyList()
                    walletOutputDao.mergeOutputsForAddress(address, Constants.ChainId.BITCOIN_CHAIN_ID, safeOutputs)
                    refreshBitcoinAmountByOutputs(walletId, address)
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

    private suspend fun refreshBitcoinAmountByOutputs(walletId: String, address: String) {
        val totalUnspent: BigDecimal = walletOutputDao.sumUnspentAmount(address, Constants.ChainId.BITCOIN_CHAIN_ID)
        val amount: String = totalUnspent.stripTrailingZeros().toPlainString()
        web3TokenDao.updateTokenAmount(walletId, Constants.ChainId.BITCOIN_CHAIN_ID, amount)
    }
}