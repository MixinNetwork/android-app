package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.api.request.MemberOrderRequest
import one.mixin.android.api.response.MemberOrder
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.setting.ui.components.MemberUpgradeContent
import one.mixin.android.ui.setting.ui.components.MemberUpgradePaymentButton
import one.mixin.android.ui.setting.ui.components.MemberUpgradeTopBar
import one.mixin.android.ui.setting.ui.components.PlanSelector
import one.mixin.android.ui.viewmodel.MemberViewModel
import one.mixin.android.vo.MemberOrderStatus
import one.mixin.android.vo.Plan
import timber.log.Timber
import one.mixin.android.api.response.Plan as ApiPlan

data class PageInitState(
    val isCheckingPendingOrder: Boolean = true,
    val isLoadingPlans: Boolean = true,
    val pendingOrder: MemberOrder? = null,
    val plansData: List<ApiPlan> = emptyList(),
    val transactionAssetId: String? = null
) {
    fun isInitialized(isGoogleBillingReady: Boolean): Boolean =
        !isCheckingPendingOrder && !isLoadingPlans && isGoogleBillingReady

    fun shouldShowLoading(isGoogleBillingReady: Boolean): Boolean =
        !isInitialized(isGoogleBillingReady)
}

data class OrderState(
    val currentOrderId: String? = null,
    val status: MemberOrderStatus? = null,
    val isPolling: Boolean = false,
    val isProcessing: Boolean = false
)

@Composable
fun MixinMemberUpgradePage(
    currentUserPlan: Plan,
    isGoogleBillingReady: Boolean,
    onClose: () -> Unit,
    onUrlGenerated: (String) -> Unit,
    onGooglePlay: (String) -> Unit
) {
    val viewModel: MemberViewModel = hiltViewModel()

    var initState by remember { mutableStateOf(PageInitState()) }
    var orderState by remember { mutableStateOf(OrderState()) }

    var selectedPlan by remember { mutableStateOf(Plan.ADVANCE) }
    var selectedPlanData by remember { mutableStateOf<ApiPlan?>(null) }

    LaunchedEffect(Unit) {
        try {
            val pendingOrder = viewModel.getLatestPendingOrder()
            val pendingPlan = pendingOrder?.let { order ->
                when (order.after) {
                    "basic" -> Plan.ADVANCE
                    "standard" -> Plan.ELITE
                    "premium" -> Plan.PROSPERITY
                    else -> Plan.ADVANCE
                }
            }

            initState = initState.copy(
                isCheckingPendingOrder = false,
                pendingOrder = pendingOrder
            )

            pendingOrder?.let { order ->
                selectedPlan = pendingPlan!!
                orderState = orderState.copy(
                    currentOrderId = order.orderId,
                    status = MemberOrderStatus.fromString(order.status),
                    isPolling = true
                )

                Timber.d("Found pending order: ${order.orderId}, plan: ${order.category}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check pending orders")
            initState = initState.copy(isCheckingPendingOrder = false)
        }
    }

    LaunchedEffect(Unit) {
        try {
            val response = viewModel.getPlans()
            if (response.isSuccess && response.data != null) {
                initState = initState.copy(
                    isLoadingPlans = false,
                    plansData = response.data!!.plans,
                    transactionAssetId = response.data!!.transaction.assetId
                )
                Timber.d("Plans loaded: ${response.data!!.plans.size}")
            } else {
                initState = initState.copy(isLoadingPlans = false)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load plans")
            initState = initState.copy(isLoadingPlans = false)
        }
    }

    LaunchedEffect(orderState.currentOrderId, orderState.isPolling) {
        if (orderState.currentOrderId != null && orderState.isPolling) {
            try {
                while (orderState.isPolling) {
                    val orderResponse = viewModel.getOrder(orderState.currentOrderId!!)
                    if (orderResponse?.isSuccess == true && orderResponse.data != null) {
                        val order = orderResponse.data!!
                        val status = MemberOrderStatus.fromString(order.status)

                        orderState = orderState.copy(status = status)

                        when (status) {
                            MemberOrderStatus.PAID, MemberOrderStatus.COMPLETED -> {
                                Timber.d("Order completed: ${order.orderId}")
                                orderState =
                                    orderState.copy(isPolling = false, isProcessing = false)
                                initState = initState.copy(pendingOrder = null)
                                onClose()
                                break
                            }

                            MemberOrderStatus.FAILED, MemberOrderStatus.EXPIRED, MemberOrderStatus.CANCEL -> {
                                Timber.d("Order failed: ${order.orderId}")
                                orderState =
                                    orderState.copy(isPolling = false, isProcessing = false)
                                initState = initState.copy(pendingOrder = null)
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to poll order status")
                orderState = orderState.copy(isPolling = false, isProcessing = false)
            }
        }
    }

    LaunchedEffect(selectedPlan, initState.plansData) {
        if (initState.plansData.isNotEmpty()) {
            selectedPlanData = mapLocalPlanToApiPlan(selectedPlan, initState.plansData)
        }
    }

    val onPaymentClick = run {
        {
            if (!initState.isInitialized(isGoogleBillingReady) || selectedPlanData == null) return@run

            val pendingOrder = initState.pendingOrder
            val isPendingSamePlan = pendingOrder != null &&
                selectedPlan == getPlanFromOrderAfter(pendingOrder.after)


            if (isPendingSamePlan && !pendingOrder?.paymentUrl.isNullOrEmpty()) {
                orderState = orderState.copy(isPolling = true)
                if (BuildConfig.IS_GOOGLE_PLAY) {
                    onGooglePlay(pendingOrder.orderId)
                } else {
                    onUrlGenerated(pendingOrder!!.paymentUrl)
                }
                return@run
            }

            if (initState.pendingOrder != null && !isPendingSamePlan) {
                return@run
            }

            orderState = orderState.copy(isProcessing = true)
            viewModel.viewModelScope.launch {
                try {
                    val planId = selectedPlanData?.plan ?: return@launch
                    // todo remove test code
                    val orderRequest = MemberOrderRequest(plan = "basic")

                    val orderResponse = viewModel.createMemberOrder(orderRequest)
                    if (orderResponse.isSuccess && orderResponse.data != null) {
                        val order = orderResponse.data!!
                        orderState = orderState.copy(
                            currentOrderId = order.orderId,
                            status = MemberOrderStatus.fromString(order.status),
                            isPolling = true,
                            isProcessing = false
                        )
                        initState = initState.copy(pendingOrder = order)
                        if (BuildConfig.IS_GOOGLE_PLAY) {
                            onGooglePlay(order.orderId)
                        } else {
                            onUrlGenerated(order.paymentUrl!!)
                        }
                    } else {
                        orderState = orderState.copy(isProcessing = false)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to create order")
                    orderState = orderState.copy(isProcessing = false)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        MemberUpgradeTopBar(onClose = onClose)

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
            selectedPlanData = selectedPlanData,
            isLoading = initState.shouldShowLoading(isGoogleBillingReady) || orderState.isProcessing,
            pendingOrder = initState.pendingOrder,
            pendingOrderPlan = initState.pendingOrder?.let {
                getPlanFromOrderAfter(it.after)
            },
            onPaymentClick = onPaymentClick
        )
    }
}

private fun mapLocalPlanToApiPlan(localPlan: Plan, apiPlans: List<ApiPlan>): ApiPlan? {
    return when (localPlan) {
        Plan.ADVANCE -> apiPlans.find { it.name == "basic" }
        Plan.ELITE -> apiPlans.find { it.name == "standard" }
        Plan.PROSPERITY -> apiPlans.find { it.name == "premium" }
        else -> apiPlans.find { it.name == "basic" }
    }
}

private fun getPlanFromOrderAfter(after: String?): Plan? {
    return when (after) {
        "basic" -> Plan.ADVANCE
        "standard" -> Plan.ELITE
        "premium" -> Plan.PROSPERITY
        else -> null
    }
}

@Preview
@Composable
private fun MixinMemberUpgradePagePreview() {
    MixinAppTheme {
        MixinMemberUpgradePage(
            currentUserPlan = Plan.ADVANCE,
            isGoogleBillingReady = true,
            onClose = {},
            onUrlGenerated = {},
            onGooglePlay = {}
        )
    }
}
