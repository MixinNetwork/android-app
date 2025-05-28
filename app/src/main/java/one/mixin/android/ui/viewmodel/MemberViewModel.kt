package one.mixin.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
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

    suspend fun createMemberOrder(request: MemberOrderRequest) =
        memberRepository.createOrder(request)

    suspend fun getPlans() = memberRepository.getPlans()

    suspend fun getOrders() =
        handleMixinResponse(
            invokeNetwork = { memberRepository.getOrders() },
            successBlock = { r -> r },
            defaultErrorHandle = {})

    suspend fun getOrder(id: String) = handleMixinResponse(invokeNetwork = {
        memberRepository.getOrder(id)
    }, successBlock = { r -> r }, defaultErrorHandle = {})

    suspend fun getLatestPendingOrder(): MemberOrder? = memberRepository.getLatestPendingOrder()

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


            handleMixinResponse(
                invokeNetwork = { memberRepository.getOrders() },
                successBlock = { resp ->
                    resp.data?.let { ordersList ->
                        memberRepository.insertOrders(ordersList)
                        _orders.value = ordersList
                    }
                },
                defaultErrorHandle = {})
        }
    }
}
