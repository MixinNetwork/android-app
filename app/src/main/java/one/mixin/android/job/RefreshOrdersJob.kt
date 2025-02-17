package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.Constants.Account
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.event.BadgeEvent
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.putInt

class RefreshOrdersJob : BaseJob(Params(PRIORITY_BACKGROUND).singleInstanceBy(GROUP).requireNetwork().persist()) {
    companion object {
        private const val serialVersionUID = 2L
        const val GROUP = "RefreshOrdersJob"
        const val LIMIT = 20
    }

    override fun onRun(): Unit =
        runBlocking {
            val lastCreate = orderDao.lastOrderCreatedAt()
            if (lastCreate != null && MixinApplication.appContext.defaultSharedPreferences.getInt(Constants.Account.PREF_HAS_USED_SWAP_TRANSACTION, -1) == -1){
                MixinApplication.appContext.defaultSharedPreferences.putInt(Constants.Account.PREF_HAS_USED_SWAP_TRANSACTION, 0)
                RxBus.publish(BadgeEvent(Account.PREF_HAS_USED_SWAP))
            }
            refreshOrders(lastCreate)
        }

    private suspend fun refreshOrders(offset: String?) {
        val response = routeService.orders(limit = LIMIT, offset = offset)
        if (response.isSuccess && response.data != null) {
            orderDao.insertListSuspend(response.data!!)
            if (response.data!!.size >= LIMIT) {
                val lastCreate = response.data?.last()?.createdAt ?: return
                refreshOrders(lastCreate)
            }
        }
    }
}
