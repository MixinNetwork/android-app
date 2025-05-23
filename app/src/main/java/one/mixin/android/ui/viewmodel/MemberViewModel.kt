package one.mixin.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.request.MemberOrderRequest
import one.mixin.android.api.response.MemberOrder
import one.mixin.android.repository.TokenRepository
import timber.log.Timber
import javax.inject.Inject

class MemberViewModel @Inject constructor(
    private val tokenRepository: TokenRepository
) : ViewModel() {

    private val _orderState = MutableStateFlow<MixinResponse<MemberOrder>?>(null)
    val orderState: StateFlow<MixinResponse<MemberOrder>?> = _orderState

    suspend fun createMemberOrder(request: MemberOrderRequest) = tokenRepository.createMemberOrder(request)

    suspend fun getPlans() = tokenRepository.getPlans()

    // Todo remove
    suspend fun createOrder() {
        val plans = getPlans().data?.plans ?: return
        val plan = plans.find { it.name == "basic" } ?: return
        val order = createMemberOrder(
            MemberOrderRequest(
                plan.plan,
                "31d2ea9c-95eb-3355-b65b-ba096853bc18",
            )
        ).data?:return
        Timber.e("Order ${order.orderId}")
    }
}
