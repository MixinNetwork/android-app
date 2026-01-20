package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.api.response.web3.WalletOutput
import one.mixin.android.db.web3.vo.isClassic
import one.mixin.android.db.web3.vo.isImported
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI
import timber.log.Timber

class RefreshWeb3BitCoinJob(val walletId: String) : BaseJob(
    Params(PRIORITY_UI_HIGH).singleInstanceBy(GROUP).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshWeb3BitCoinJob"
    }

    override fun onRun(): Unit = runBlocking {
        val wallet = web3WalletDao.getWalletById(walletId) ?: return@runBlocking
        if (wallet.isImported() || wallet.isClassic()) {
            val address = web3AddressDao.getAddressesByChainId(walletId, Constants.ChainId.BITCOIN_CHAIN_ID) ?: return@runBlocking
            fetchBtcOutputs(walletId, address.destination)
        }
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
                    refreshBitcoinTokenAmountByOutputs(walletId, address)
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
}