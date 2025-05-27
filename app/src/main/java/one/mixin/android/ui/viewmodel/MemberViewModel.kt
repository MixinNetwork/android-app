package one.mixin.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.MemberOrderRequest
import one.mixin.android.api.response.MemberOrder
import one.mixin.android.repository.TokenRepository
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MemberViewModel @Inject constructor(
    private val tokenRepository: TokenRepository
) : ViewModel() {

    private val _orderState = MutableStateFlow<MixinResponse<MemberOrder>?>(null)
    val orderState: StateFlow<MixinResponse<MemberOrder>?> = _orderState

    suspend fun createMemberOrder(request: MemberOrderRequest) = tokenRepository.createMemberOrder(request)

    suspend fun getPlans() = tokenRepository.getPlans()

    suspend fun getOrders() = tokenRepository.getOrders()

    suspend fun getOrder(id: String) = tokenRepository.getOrder(id)

    suspend fun createOrder(onResult: (String?) -> Unit) {
        val plans = getPlans().data?.plans ?: return onResult(null)
        val plan = plans.find { it.name == "basic" } ?: return onResult(null)
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
