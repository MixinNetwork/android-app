package one.mixin.android.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.MemberOrderRequest
import one.mixin.android.api.response.MemberOrder
import one.mixin.android.api.service.MemberService
import one.mixin.android.db.MemberOrderDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberRepository @Inject constructor(
    private val memberService: MemberService,
    private val orderDao: MemberOrderDao
) {
    suspend fun createOrder(request: MemberOrderRequest): MixinResponse<MemberOrder> {
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

    suspend fun insertOrders(orders: List<MemberOrder>) {
        orderDao.insertListSuspend(orders)
    }
    
    suspend fun getAllMemberOrders(): List<MemberOrder> {
        return orderDao.getAllOrders()
    }

    suspend fun getLatestPendingOrder(): MemberOrder? {
        return orderDao.getLatestPendingOrder()
    }
}
