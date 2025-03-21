package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.ui.wallet.fiatmoney.requestRouteAPI

class RefreshWeb3TransactionJob(
    val hash: String,
) : BaseJob(Params(PRIORITY_UI_HIGH).requireNetwork().setGroupId(GROUP)) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshWeb3TransactionJob"

    }

    override fun onRun(): Unit =
        runBlocking {
            requestRouteAPI(
                invokeNetwork = {
                    routeService.getTransaction(hash)
                },
                successBlock = { response ->
                    val result = response.data
                    web3TransactionDao.insert(result!!)
                },
                failureBlock = { response ->
                    false
                },
                requestSession = {
                    userService.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID))
                },
                defaultErrorHandle = {}
            )
        }
}
