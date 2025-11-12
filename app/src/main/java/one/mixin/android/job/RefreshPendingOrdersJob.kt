package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RefreshPendingOrdersJob(
    val walletId: String?,
) : BaseJob(Params(PRIORITY_BACKGROUND).singleInstanceBy(GROUP).requireNetwork().persist()) {
    companion object {
        private const val serialVersionUID = 2L
        const val GROUP = "RefreshPendingOrdersJob"
    }

    override fun onRun(): Unit =
        runBlocking {
            val pendingOrders = swapOrderDao.getPendingOrders()
            if (pendingOrders.isNotEmpty()) {
                pendingOrders.forEach {
                    launch {
                        refreshPendingOrder(it.orderId)
                    }
                }
            }
        }

    private suspend fun refreshPendingOrder(orderId: String) {
        val response = routeService.orderById(orderId, walletId)
        if (response.isSuccess && response.data != null) {
            swapOrderDao.insertSuspend(response.data!!)
        }
    }
}
