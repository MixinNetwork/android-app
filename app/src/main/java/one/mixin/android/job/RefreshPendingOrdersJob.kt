package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RefreshPendingOrdersJob : BaseJob(Params(PRIORITY_BACKGROUND).singleInstanceBy(GROUP).requireNetwork().persist()) {
    companion object {
        private const val serialVersionUID = 2L
        const val GROUP = "RefreshOrdersJob"
    }

    override fun onRun(): Unit =
        runBlocking {
            val pendingOrders = orderDao.getPendingOrders()
            if (pendingOrders.isNotEmpty()) {
                pendingOrders.forEach {
                    launch {
                        refreshPendingOrders(it.createdAt)
                    }
                }
            }
        }

    private suspend fun refreshPendingOrders(offset: String) {
        val response = routeService.orders(limit = 1, offset = offset)
        if (response.isSuccess && response.data != null) {
            orderDao.insertListSuspend(response.data!!)
        }
    }
}
