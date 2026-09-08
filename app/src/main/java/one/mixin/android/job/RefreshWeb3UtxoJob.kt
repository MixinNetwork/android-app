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

open class RefreshWeb3UtxoJob(
    private val walletId: String,
    private val assetId: String,
) : BaseJob(
    Params(PRIORITY_UI_HIGH).singleInstanceBy(GROUP + walletId + assetId).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshWeb3UtxoJob"
    }

    override fun onRun(): Unit = runBlocking {
        if (assetId !in Constants.Web3UtxoChainIds) return@runBlocking
        val wallet = web3WalletDao.getWalletById(walletId) ?: return@runBlocking
        if (wallet.isImported() || wallet.isClassic()) {
            val address = web3AddressDao.getAddressesByChainId(walletId, assetId) ?: return@runBlocking
            fetchOutputs(walletId, address.destination, assetId)
        }
    }

    private suspend fun fetchOutputs(walletId: String, address: String, assetId: String) {
        requestRouteAPI(
            invokeNetwork = {
                routeService.getWalletOutputs(walletId = walletId, address = address, assetId = assetId)
            },
            successBlock = { response ->
                val outputs = response.data
                try {
                    val safeOutputs: List<WalletOutput> = (outputs ?: emptyList()).filter { it.assetId == assetId }
                    walletOutputDao.mergeOutputsForAddress(address, assetId, safeOutputs)
                    refreshUtxoTokenAmountByOutputs(walletId, address, assetId)
                    Timber.d("Merged ${safeOutputs.size} $assetId outputs into database for walletId=$walletId")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to insert $assetId outputs for walletId=$walletId into DB")
                }
            },
            failureBlock = { response ->
                Timber.e("Failed to fetch $assetId outputs for walletId=$walletId address=$address: ${response.errorCode} - ${response.errorDescription}")
                false
            },
            requestSession = {
                userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
            },
            defaultErrorHandle = {}
        )
    }
}
