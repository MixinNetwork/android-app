package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.api.request.MemberOrderRequest
import one.mixin.android.api.response.MemberOrderPlan
import one.mixin.android.api.response.MembershipOrder

import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.setting.ui.components.MemberUpgradeContent
import one.mixin.android.ui.setting.ui.components.MemberUpgradePaymentButton
import one.mixin.android.ui.setting.ui.components.MemberUpgradeTopBar
import one.mixin.android.ui.setting.ui.components.PlanSelector
import one.mixin.android.ui.viewmodel.MemberViewModel
import one.mixin.android.util.ErrorHandler
import one.mixin.android.vo.MemberOrderStatus
import one.mixin.android.vo.Plan
import timber.log.Timber

data class PlanPurchaseState(
    val currentPlan: Plan? = null,
    val isBillingManagerInitialized: Boolean = BuildConfig.IS_GOOGLE_PLAY.not(),
    val availablePlans: List<MemberOrderPlan> = emptyList(),
    val error: String? = null,
    val availablePlayStorePlans: Set<String> = emptySet(),
    val loading: Boolean = false
) {
    val isLoading: Boolean
        get() = (error == null && availablePlans.isEmpty() && isBillingManagerInitialized) || loading
}

@Composable
fun MixinMemberUpgradePage(
    currentUserPlan: Plan,
    selectedPlanOverride: Plan? = null,
    onClose: () -> Unit,
    onUrlGenerated: (String) -> Unit,
    onGooglePlay: (orderId: String, playStoreSubscriptionId: String) -> Unit,
    onContactTeamMixin: () -> Unit = {},
    onViewInvoice: (MembershipOrder) -> Unit = {}
) {
    val viewModel: MemberViewModel = hiltViewModel()

    val pendingOrderState by viewModel.pendingOrder.collectAsState()
    val subscriptionPlans by viewModel.subscriptionPlans.collectAsState()

    MixinMemberUpgradePageContent(
        currentUserPlan = currentUserPlan,
        selectedPlanOverride = selectedPlanOverride,
        pendingOrderState = pendingOrderState,
        subscriptionPlans = subscriptionPlans,
        onClose = onClose,
        onUrlGenerated = onUrlGenerated,
        onGooglePlay = onGooglePlay,
        onContactTeamMixin = onContactTeamMixin,
        onViewInvoice = onViewInvoice,
        getPlans = { viewModel.getPlans() },
        getOrder = { viewModel.getOrder(it) },
        insertOrders = { viewModel.insertOrders(it) },
        createMemberOrder = { viewModel.createMemberOrder(it) },
        clearPendingOrder = { viewModel.clearPendingOrder() }
    )
}

@Composable
fun MixinMemberUpgradePageContent(
    currentUserPlan: Plan,
    selectedPlanOverride: Plan? = null,
    pendingOrderState: MembershipOrder? = null,
    subscriptionPlans: List<MemberOrderPlan> = emptyList(),
    onClose: () -> Unit,
    onUrlGenerated: (String) -> Unit,
    onGooglePlay: (orderId: String, playStoreSubscriptionId: String) -> Unit,
    onContactTeamMixin: () -> Unit = {},
    onViewInvoice: (MembershipOrder) -> Unit = {},
    getPlans: suspend () -> one.mixin.android.api.response.MixinResponse<one.mixin.android.api.response.MemberOrderPlans>,
    getOrder: suspend (String) -> one.mixin.android.api.response.MixinResponse<MembershipOrder>?,
    insertOrders: suspend (MembershipOrder) -> Unit,
    createMemberOrder: suspend (MemberOrderRequest) -> one.mixin.android.api.response.MixinResponse<MembershipOrder>,
    clearPendingOrder: () -> Unit
) {
    var purchaseState by remember { mutableStateOf(PlanPurchaseState()) }
    var savedOrderId by remember { mutableStateOf<String?>(null) }

    var selectedPlan by remember {
        mutableStateOf(
            selectedPlanOverride?.let { p ->
                if (p != Plan.None) p else Plan.ADVANCE
            } ?: when (currentUserPlan) {
                Plan.None -> Plan.ADVANCE
                Plan.ADVANCE -> Plan.ADVANCE
                Plan.ELITE -> Plan.ELITE
                Plan.PROSPERITY -> Plan.PROSPERITY
            }
        )
    }

    LaunchedEffect(Unit) {
        try {
            val response = getPlans()
            if (response.isSuccess && response.data != null) {
                val availablePlayStorePlans = if (BuildConfig.IS_GOOGLE_PLAY) {
                    val billingPlanIds = subscriptionPlans.map { it.planId }.toSet()
                    response.data!!.plans
                        .mapNotNull { it.playStoreSubscriptionId }
                        .filter { billingPlanIds.contains(it) }
                        .toSet()
                } else {
                    emptySet()
                }

                purchaseState = purchaseState.copy(
                    availablePlans = response.data!!.plans,
                    availablePlayStorePlans = availablePlayStorePlans
                )
                Timber.d("Plans loaded: ${response.data!!.plans.size}, Valid Google Play plans: ${availablePlayStorePlans.size}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load plans")
            purchaseState = purchaseState.copy(
                error = ErrorHandler.getErrorMessage(e)
            )
        }
    }

    LaunchedEffect(pendingOrderState?.orderId ?: "") {
        try {
            while (pendingOrderState?.orderId.isNullOrEmpty().not()) {
                val orderResponse = getOrder(pendingOrderState!!.orderId)
                if (orderResponse?.isSuccess == true && orderResponse.data != null) {
                    val order = orderResponse.data!!
                    val status = MemberOrderStatus.fromString(order.status)

                    when (status) {
                        MemberOrderStatus.PAID, MemberOrderStatus.COMPLETED -> {
                            Timber.d("Order completed: ${order.orderId}")
                            insertOrders(order)
                            onClose()
                            break
                        }
                        MemberOrderStatus.PENDING -> {
                            Timber.d("Order still pending")
                        }
                        else -> {
                            Timber.d("Order status: $status")
                            break
                        }
                    }
                }
                delay(2000)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking order status")
        }
    }

    LaunchedEffect(savedOrderId ?: "") {
        try {
            while (savedOrderId.isNullOrEmpty().not()) {
                val orderResponse = getOrder(savedOrderId!!)
                if (orderResponse?.isSuccess == true && orderResponse.data != null) {
                    val order = orderResponse.data!!
                    val status = MemberOrderStatus.fromString(order.status)

                    if (status == MemberOrderStatus.PAID || status == MemberOrderStatus.COMPLETED) {
                        insertOrders(order)
                        onClose()
                        break
                    }
                }
                delay(3000)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking saved order status")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        MemberUpgradeTopBar(onClose = onClose)

        MemberUpgradeContent(
            currentUserPlan = currentUserPlan,
            purchaseState = purchaseState,
            onContactTeamMixin = onContactTeamMixin
        )

        Spacer(modifier = Modifier.height(24.dp))

        PlanSelector(
            availablePlans = purchaseState.availablePlans,
            selectedPlan = selectedPlan,
            onPlanSelected = { selectedPlan = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        val scope = rememberCoroutineScope()
        MemberUpgradePaymentButton(
            selectedPlan = selectedPlan,
            purchaseState = purchaseState,
            onClick = {
                val plan = purchaseState.availablePlans.find { Plan.fromString(it.name) == selectedPlan }
                if (plan != null) {
                    val orderRequest = MemberOrderRequest(planId = plan.planId)
                    purchaseState = purchaseState.copy(loading = true)
                    scope.launch(CoroutineExceptionHandler { _, error ->
                        Timber.e(error, "Error creating order")
                        purchaseState = purchaseState.copy(loading = false, error = error.message)
                    }) {
                        val orderResponse = createMemberOrder(orderRequest)
                        if (orderResponse.isSuccess && orderResponse.data != null) {
                            val order = orderResponse.data!!
                            if (order.status == MemberOrderStatus.PAID.name || order.status == MemberOrderStatus.COMPLETED.name) {
                                onViewInvoice(order)
                            } else if (order.payUrl.isNullOrEmpty().not()) {
                                onUrlGenerated(order.payUrl!!)
                            } else if (order.playStoreSubscriptionId.isNullOrEmpty().not()) {
                                onGooglePlay(order.orderId, order.playStoreSubscriptionId!!)
                                savedOrderId = order.orderId
                            }
                        } else {
                            purchaseState = purchaseState.copy(loading = false, error = orderResponse.error?.description ?: "Unknown error")
                        }
                        purchaseState = purchaseState.copy(loading = false)
                    }
                }
            }
        )
    }
}

@Preview
@Composable
private fun MixinMemberUpgradePagePreview() {
    MixinAppTheme {
        MixinMemberUpgradePageContent(
            currentUserPlan = Plan.ADVANCE,
            selectedPlanOverride = null,
            onClose = {},
            onUrlGenerated = {},
            onGooglePlay = { _, _ -> },
            onContactTeamMixin = {},
            getPlans = { one.mixin.android.api.response.MixinResponse() },
            getOrder = { null },
            insertOrders = {},
            createMemberOrder = { one.mixin.android.api.response.MixinResponse() },
            clearPendingOrder = {}
        )
    }
}
