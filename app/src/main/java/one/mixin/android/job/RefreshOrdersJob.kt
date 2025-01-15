package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class RefreshOrdersJob : BaseJob(Params(PRIORITY_BACKGROUND).singleInstanceBy(GROUP).requireNetwork().persist()) {
    companion object {
        private const val serialVersionUID = 2L
        const val GROUP = "RefreshOrdersJob"
        const val LIMIT = 20
    }

    override fun onRun(): Unit =
        runBlocking {
            val lastCreate = orderDao.lastOrderCreatedAt()
            refreshOrders(lastCreate)
        }

    private suspend fun refreshOrders(offset: String?) {
        val response = routeService.orders(limit = LIMIT, offset = offset)
        if (response.isSuccess && response.data != null) {
            Timber.e("RefreshOrdersJob: ${response.data!!.size}")
            orderDao.insertListSuspend(response.data!!)
            if (response.data!!.size >= LIMIT) {
                val lastCreate = response.data?.last()?.createdAt ?: return
                refreshOrders(lastCreate)
            }
        }
    }
}
