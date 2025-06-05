package one.mixin.android.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.Constants.RouteConfig.SAFE_BOT_USER_ID
import one.mixin.android.RxBus
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.MemberOrderRequest
import one.mixin.android.api.request.RelationshipAction
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.api.response.MemberOrder
import one.mixin.android.billing.BillingManager
import one.mixin.android.billing.SubscriptionProcessStatus
import one.mixin.android.event.MembershipEvent
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAccountJob
import one.mixin.android.job.UpdateRelationshipJob
import one.mixin.android.repository.MemberRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.vo.MemberOrderStatus
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MemberViewModel @Inject constructor(
    private val jobManager: MixinJobManager,
    private val userRepository: UserRepository,
    private val memberRepository: MemberRepository,
    application: Application
) : ViewModel() {

    private val _orderState = MutableStateFlow<MixinResponse<MemberOrder>?>(null)
    val orderState: StateFlow<MixinResponse<MemberOrder>?> = _orderState

    private val billingManager = BillingManager.getInstance(application, viewModelScope)

    val subscriptionStatus: StateFlow<SubscriptionProcessStatus> = billingManager.subscriptionStatus

    val isGoogleBillingReady = subscriptionStatus.map { status ->
        status != SubscriptionProcessStatus.Loading
    }

    fun subscribe(activity: Activity) {
        billingManager.launchSubscriptionFlow(activity)
    }

    fun subscribe100(activity: Activity, orderId: String? = null) {
        billingManager.launchSubscriptionFlow(activity, BillingManager.PRODUCT_ID, BillingManager.PLAN_ID_100, orderId)
    }

    fun refreshSubscriptionStatus() {
        billingManager.refresh()
    }

    override fun onCleared() {
        super.onCleared()
        billingManager.destroy()
    }

    suspend fun createMemberOrder(request: MemberOrderRequest): MixinResponse<MemberOrder> {
        viewModelScope.launch(Dispatchers.IO) {
            val bot = userRepository.getUserById(SAFE_BOT_USER_ID)
            if (bot == null || bot.relationship != "FRIEND") {
                jobManager.addJobInBackground(UpdateRelationshipJob(RelationshipRequest(SAFE_BOT_USER_ID, RelationshipAction.ADD.name)))
            }
        }
        return memberRepository.createOrder(request)
    }

    suspend fun getPlans() = memberRepository.getPlans()

    suspend fun getOrders() =
        handleMixinResponse(
            invokeNetwork = { memberRepository.getOrders() },
            successBlock = { r -> r },
            defaultErrorHandle = {})

    suspend fun getOrder(id: String) = handleMixinResponse(invokeNetwork = {
        memberRepository.getOrder(id)
    }, successBlock = { r ->
        jobManager.addJobInBackground(RefreshAccountJob())
        r
    }, defaultErrorHandle = {})

    private val _pendingOrder = MutableStateFlow<MemberOrder?>(null)
    val pendingOrder: StateFlow<MemberOrder?> = _pendingOrder

    init {
        viewModelScope.launch {
            memberRepository.getLatestPendingOrderFlow().collect { order ->
                _pendingOrder.value = order
            }
        }
    }

    suspend fun loadOrders() {
        viewModelScope.launch {
            handleMixinResponse(
                invokeNetwork = { memberRepository.getOrders() },
                successBlock = { resp ->
                    resp.data?.let { ordersList ->
                        memberRepository.insertOrders(ordersList)
                    }
                },
                defaultErrorHandle = {})
        }
    }

    fun getAllMemberOrders() = memberRepository.getAllMemberOrders()

    fun insertOrders(order: MemberOrder) {
        viewModelScope.launch {
            memberRepository.insertOrder(order)
        }
    }
}
