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

    val pendingOrderState by viewModel.pendingOrder.collectAsState()
    val subscriptionPlans by viewModel.subscriptionPlans.collectAsState()

    LaunchedEffect(Unit) {
        try {
            val response = viewModel.getPlans()
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

                if (BuildConfig.IS_GOOGLE_PLAY) {
                    val billingPlanIds = subscriptionPlans.map { it.planId }
                    val backendPlayStoreIds = response.data!!.plans.mapNotNull { it.playStoreSubscriptionId }

                    Timber.d("Billing library plan IDs: $billingPlanIds")
                    Timber.d("Backend Play Store subscription IDs: $backendPlayStoreIds")
                    Timber.d("Matched plan IDs: $availablePlayStorePlans")
                }
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
                val orderResponse = viewModel.getOrder(pendingOrderState!!.orderId)
                if (orderResponse?.isSuccess == true && orderResponse.data != null) {
                    val order = orderResponse.data!!
                    val status = MemberOrderStatus.fromString(order.status)

                    when (status) {
                        MemberOrderStatus.PAID, MemberOrderStatus.COMPLETED -> {
                            Timber.d("Order completed: ${order.orderId}")
                            viewModel.insertOrders(order)
                            onClose()
                            break
                        }

                        MemberOrderStatus.FAILED, MemberOrderStatus.EXPIRED, MemberOrderStatus.CANCEL -> {
                            Timber.d("Order failed: ${order.orderId}")
                            viewModel.insertOrders(order)
                            onClose()
                            break
                        }

                        else -> {
                            Timber.d("Order pending: ${order.orderId}")
                            delay(3000)
                        }
                    }
                } else {
                    delay(3000)
                }
            }
        } catch (e: Exception) {
            purchaseState.copy(error = ErrorHandler.getErrorMessage(e))
            Timber.e(e, "Failed to poll order status")
        }
    }

    MixinAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
        {
            MemberUpgradeTopBar(onClose = onClose)
            Spacer(modifier = Modifier.height(16.dp))

            PlanSelector(
                selectedPlan = selectedPlan,
                onPlanSelected = { plan ->
                    selectedPlan = plan
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                MemberUpgradeContent(selectedPlan = selectedPlan)
            }

            MemberUpgradePaymentButton(
                currentUserPlan = currentUserPlan,
                selectedPlan = selectedPlan,
                pendingOrder = pendingOrderState,
                purchaseState = purchaseState,
                savedOrderId = savedOrderId,
                onPaymentClick = {
                    val isGooglePlayChannel = BuildConfig.IS_GOOGLE_PLAY
                    val plan =
                        mapLocalPlanToMemberOrderPlan(selectedPlan, purchaseState.availablePlans)
                            ?: return@MemberUpgradePaymentButton
                    viewModel.viewModelScope.launch(CoroutineExceptionHandler { _, error ->
                        purchaseState = purchaseState.copy(loading = false)
                        purchaseState = purchaseState.copy(
                            error = ErrorHandler.getErrorMessage(error)
                        )
                    }) {
                        purchaseState = purchaseState.copy(loading = true)
                        val orderRequest = if (isGooglePlayChannel) {
                            MemberOrderRequest(plan = plan.plan, fiatSource = "play_store", subscriptionId = plan.playStoreSubscriptionId)
                        } else {
                            MemberOrderRequest(plan = plan.plan)
                        }
                        val orderResponse = viewModel.createMemberOrder(orderRequest)

                        if (orderResponse.isSuccess && orderResponse.data != null) {
                            orderResponse.data?.orderId?.let { orderId ->
                                savedOrderId = orderId
                            }

                            if (isGooglePlayChannel) {
                                plan.playStoreSubscriptionId?.let { playStoreId ->
                                    onGooglePlay(orderResponse.data!!.orderId!!, playStoreId)
                                }
                            } else {
                                onUrlGenerated(orderResponse.data!!.paymentUrl!!)
                            }
                        }
                        purchaseState = purchaseState.copy(loading = false)
                    }
                },
                onContactSupport = onContactTeamMixin,
                onViewInvoice = onViewInvoice
            )
        }
    }
}

@Preview
@Composable
private fun MixinMemberUpgradePagePreview() {
    MixinAppTheme {
        MixinMemberUpgradePage(
            currentUserPlan = Plan.ADVANCE,
            selectedPlanOverride = null,
            onClose = {},
            onUrlGenerated = {},
            onGooglePlay = { _, _ -> },
            onContactTeamMixin = {}
        )
    }
}
