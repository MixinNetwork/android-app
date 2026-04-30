package one.mixin.android.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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

    fun allLimitOrders(filterParams: OrderFilterParams): Flow<PagingData<OrderItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = Constants.PAGE_SIZE,
                prefetchDistance = Constants.PAGE_SIZE * 2,
                enablePlaceholders = true,
            ),
            pagingSourceFactory = { LimitOrderDataProvider.allOrders(database, filterParams) }
        ).flow
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
