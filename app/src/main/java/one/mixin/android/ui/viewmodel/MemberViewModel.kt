package one.mixin.android.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import one.mixin.android.Constants.RouteConfig.SAFE_BOT_USER_ID
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.MemberOrderRequest
import one.mixin.android.api.request.RelationshipAction
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.api.response.MembershipOrder
import one.mixin.android.billing.BillingManager
import one.mixin.android.billing.SubscriptionPlanInfo
import one.mixin.android.billing.SubscriptionProcessStatus
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshAccountJob
import one.mixin.android.job.UpdateRelationshipJob
import one.mixin.android.repository.MemberRepository
import one.mixin.android.repository.UserRepository
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MemberViewModel @Inject constructor(
    private val jobManager: MixinJobManager,
    private val userRepository: UserRepository,
    private val memberRepository: MemberRepository,
    application: Application
) : ViewModel() {

    private val _orderState = MutableStateFlow<MixinResponse<MembershipOrder>?>(null)
    val orderState: StateFlow<MixinResponse<MembershipOrder>?> = _orderState

    private val billingManager = BillingManager.getInstance(application, viewModelScope)

    val subscriptionPlans: StateFlow<List<SubscriptionPlanInfo>> = billingManager.subscriptionPlans

    val subscriptionStatus: StateFlow<SubscriptionProcessStatus> = billingManager.subscriptionStatus

    val isGoogleBillingReady = subscriptionStatus.map { status ->
        status != SubscriptionProcessStatus.Loading
    }

    fun subscribe(activity: Activity) {
        billingManager.launchSubscriptionFlow(activity)
    }

    fun subscribeWithPlanId(activity: Activity, orderId: String, planId: String) {
        Timber.d("Launching subscription with planId: $planId, orderId: $orderId")
        billingManager.launchSubscriptionFlow(activity, BillingManager.PRODUCT_ID, planId, orderId)
    }

    fun refreshSubscriptionStatus() {
        billingManager.refresh()
    }

    suspend fun refreshUser(userId: String) = userRepository.refreshUser(userId)

    override fun onCleared() {
        super.onCleared()
        billingManager.destroy()
    }

    suspend fun createMemberOrder(request: MemberOrderRequest): MixinResponse<MembershipOrder> {
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

    private val _pendingOrder = MutableStateFlow<MembershipOrder?>(null)
    val pendingOrder: StateFlow<MembershipOrder?> = _pendingOrder

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

    fun getOrdersFlow(orderId: String) = memberRepository.getOrdersFlow(orderId)

    fun insertOrders(order: MembershipOrder) {
        viewModelScope.launch {
            memberRepository.insertOrder(order)
        }
    }

    suspend fun cancelOrder(orderId: String) = memberRepository.cancelOrder(orderId)
}
