package one.mixin.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.MemberOrderRequest
import one.mixin.android.api.response.MemberOrder
import one.mixin.android.repository.MemberRepository
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MemberViewModel @Inject constructor(
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _orderState = MutableStateFlow<MixinResponse<MemberOrder>?>(null)
    val orderState: StateFlow<MixinResponse<MemberOrder>?> = _orderState

    private val _orders = MutableStateFlow<List<MemberOrder>>(emptyList())
    val orders: StateFlow<List<MemberOrder>> = _orders

    suspend fun createMemberOrder(request: MemberOrderRequest) = memberRepository.createOrder(request)

    suspend fun getPlans() = memberRepository.getPlans()

    suspend fun getOrders() = memberRepository.getOrders()

    suspend fun getOrder(id: String) = memberRepository.getOrder(id)

    suspend fun loadOrders() {
        viewModelScope.launch {
            try {
                val localOrders = memberRepository.getAllMemberOrders()
                if (localOrders.isNotEmpty()) {
                    _orders.value = localOrders
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load local orders")
            }

            val response = memberRepository.getOrders()
            if (response.isSuccess && response.data != null) {
                val ordersList = response.data
                if (ordersList != null) {
                    memberRepository.insertOrders(ordersList)
                    _orders.value = ordersList
                }
            }
        }
    }

    suspend fun createOrder(onResult: (String?) -> Unit) {
        val plans = getPlans().data?.plans ?: return onResult(null)
        val plan = plans.find { it.name == "basic" } ?: return onResult(null)
        // Todo remove test code
        val order = createMemberOrder(
            MemberOrderRequest(
                plan = plan.plan,
                asset = "4d8c508b-91c5-375b-92b0-ee702ed2dac5",
            )
        ).data ?: return onResult(null)

        Timber.e("Order ${order.orderId}")
        onResult(order.paymentUrl)
    }
}
