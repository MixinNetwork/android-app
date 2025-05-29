package one.mixin.android.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.MemberOrderRequest
import one.mixin.android.api.response.MemberOrder
import one.mixin.android.billing.BillingManager
import one.mixin.android.billing.SubscriptionProcessStatus
import one.mixin.android.repository.MemberRepository
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MemberViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    application: Application
) : ViewModel() {

    private val _orderState = MutableStateFlow<MixinResponse<MemberOrder>?>(null)
    val orderState: StateFlow<MixinResponse<MemberOrder>?> = _orderState

    private val _orders = MutableStateFlow<List<MemberOrder>>(emptyList())
    val orders: StateFlow<List<MemberOrder>> = _orders

    private val billingManager = BillingManager.getInstance(application, viewModelScope)

    val subscriptionStatus: StateFlow<SubscriptionProcessStatus> = billingManager.subscriptionStatus
    val productDetails: StateFlow<ProductDetails?> = billingManager.productDetails

    fun subscribe(activity: Activity) {
        billingManager.launchSubscriptionFlow(activity)
    }

    fun refreshSubscriptionStatus() {
        billingManager.refresh()
    }

    override fun onCleared() {
        super.onCleared()
        billingManager.destroy()
    }

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

    // 处理订阅购买完成后的操作
    fun handlePurchaseComplete(purchaseToken: String, productId: String) {
        viewModelScope.launch {
            try {
                // TODO: 将购买令牌发送到后端进行验证
                // 示例: memberRepository.verifyPurchase(purchaseToken, productId)
                Timber.i("Purchase completed: Token=$purchaseToken, Product=$productId")

                // 刷新订单状态
                loadOrders()
            } catch (e: Exception) {
                Timber.e(e, "Failed to handle purchase completion")
            }
        }
    }

    // 根据订阅状态判断用户权限
    fun hasActiveSubscription(): Boolean {
        return subscriptionStatus.value is SubscriptionProcessStatus.Subscribed
    }
}
