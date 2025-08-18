package one.mixin.android.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.MemberOrderRequest
import one.mixin.android.api.response.MembershipOrder
import one.mixin.android.api.service.MemberService
import one.mixin.android.db.MembershipOrderDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberRepository @Inject constructor(
    private val memberService: MemberService,
    private val orderDao: MembershipOrderDao
) {
    suspend fun createOrder(request: MemberOrderRequest): MixinResponse<MembershipOrder> {
        val response = memberService.createOrder(request)
        if (response.isSuccess) {
            response.data?.let { order ->
                withContext(Dispatchers.IO) {
                    orderDao.insert(order)
                }
            }
        }
        return response
    }

    suspend fun getPlans() = memberService.getPlans()

    suspend fun createMemberOrder(request: MemberOrderRequest) = memberService.createOrder(request)

    suspend fun getOrders() = memberService.getOrders()

    suspend fun getOrder(id: String) = memberService.getOrder(id)

    suspend fun insertOrders(orders: List<MembershipOrder>) {
        orderDao.insertListSuspend(orders)
    }

    suspend fun insertOrder(order: MembershipOrder) {
        orderDao.insertSuspend(order)
    }
    
    fun getAllMemberOrders() = orderDao.getAllOrdersFlow()

    fun getOrdersFlow(orderId:String) = orderDao.getOrdersFlow(orderId)

    fun getLatestPendingOrderFlow(): Flow<MembershipOrder?> {
        return orderDao.getLatestPendingOrderFlow()
    }

    suspend fun cancelOrder(id: String): MixinResponse<MembershipOrder> {
        return memberService.cancelOrder(id)
    }
}
