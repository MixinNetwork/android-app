package one.mixin.android.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.api.service.RouteService
import one.mixin.android.db.WalletDatabase
import one.mixin.android.db.provider.LimitOrderDataProvider
import one.mixin.android.vo.route.OrderItem
import javax.inject.Inject

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val database: WalletDatabase,
    private val routeService: RouteService,
) : ViewModel() {

    fun allLimitOrders(initialLoadKey: Int? = 0, filterParams: OrderFilterParams): LiveData<PagedList<OrderItem>> {
        val factory = LimitOrderDataProvider.allOrders(database, filterParams)
        val config = PagedList.Config.Builder()
            .setPrefetchDistance(Constants.PAGE_SIZE * 2)
            .setPageSize(Constants.PAGE_SIZE)
            .setEnablePlaceholders(true)
            .build()
        return LivePagedListBuilder(factory, config).setInitialLoadKey(initialLoadKey).build()
    }

    suspend fun refreshPendingOrders(): Boolean = withContext(Dispatchers.IO) {
        val orderDao = database.orderDao()
        val pendingOrders = orderDao.getPendingOrders() + orderDao.getCancellingOrders()
        if (pendingOrders.isEmpty()) return@withContext false
        val ids = pendingOrders.map { it.orderId }
        val resp = routeService.getLimitOrders(ids)
        if (resp.isSuccess && resp.data != null) {
            orderDao.insertListSuspend(resp.data!!)
        }
        return@withContext true
    }
}
