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

@Composable
fun MixinMemberUpgradePage(
    currentUserPlan: Plan,
    onClose: () -> Unit,
    onUrlGenerated: (String) -> Unit
) {
    val viewModel: MemberViewModel = hiltViewModel()
    var selectedPlan by remember { mutableStateOf(Plan.ADVANCE) }
    var selectedPlanData by remember { mutableStateOf<ApiPlan?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingPlans by remember { mutableStateOf(true) }
    var plansData by remember { mutableStateOf<List<ApiPlan>>(emptyList()) }
    var transactionAssetId by remember { mutableStateOf<String?>(null) }
    var currentOrderId by remember { mutableStateOf<String?>(null) }
    var isPollingOrder by remember { mutableStateOf(false) }
    var currentOrderStatus by remember { mutableStateOf<MemberOrderStatus?>(null) }
    var pendingOrder by remember { mutableStateOf<MemberOrder?>(null) }
    var pendingOrderPlan by remember { mutableStateOf<Plan?>(null) }
    var isCheckingPendingOrder by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isCheckingPendingOrder = true
        try {
            val latestPending = viewModel.getLatestPendingOrder()
            latestPending?.let {
                pendingOrder = it
                currentOrderId = it.orderId
                currentOrderStatus = MemberOrderStatus.fromString(it.status)
                isPollingOrder = true

                pendingOrderPlan = when (it.category) {
                    "basic" -> Plan.ADVANCE
                    "standard" -> Plan.ELITE
                    "premium" -> Plan.PROSPERITY
                    else -> Plan.ADVANCE
                }

                selectedPlan = pendingOrderPlan!!

                Timber.d("Found pending order: ${it.orderId}, status: ${it.status}, plan: ${it.category}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check pending orders")
        } finally {
            isCheckingPendingOrder = false
        }
    }

    LaunchedEffect(currentOrderId) {
        if (currentOrderId != null && isPollingOrder) {
            try {
                while (isPollingOrder) {
                    val orderResponse = viewModel.getOrder(currentOrderId!!)
                    if (orderResponse?.isSuccess == true && orderResponse?.data != null) {
                        val order = orderResponse.data!!
                        val orderStatus = MemberOrderStatus.fromString(order.status)
                        currentOrderStatus = orderStatus

                        when (orderStatus) {
                            MemberOrderStatus.PAID, MemberOrderStatus.COMPLETED -> {
                                Timber.d("Order completed: ${order.orderId}, status: ${orderStatus.value}")
                                isPollingOrder = false
                                isLoading = false
                                pendingOrder = null
                                onClose()
                            }
                            MemberOrderStatus.FAILED, MemberOrderStatus.EXPIRED, MemberOrderStatus.CANCEL -> {
                                Timber.d("Order failed: ${order.orderId}, status: ${orderStatus.value}")
                                isPollingOrder = false
                                isLoading = false
                                pendingOrder = null
                            }
                            else -> {
                                Timber.d("Order pending: ${order.orderId}, status: ${orderStatus.value}")
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
                isPollingOrder = false
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        isLoadingPlans = true
        try {
            val response = viewModel.getPlans()
            if (response.isSuccess && response.data != null) {
                plansData = response.data!!.plans
                transactionAssetId = response.data!!.transaction.assetId

                if (pendingOrderPlan != null) {
                    selectedPlan = pendingOrderPlan!!
                }

                selectedPlanData = mapLocalPlanToApiPlan(selectedPlan, plansData)
                Timber.d("Plans loaded: ${plansData.size}, selected: ${selectedPlanData?.name}")
            }
        } finally {
            isLoadingPlans = false
        }
    }

    LaunchedEffect(selectedPlan, plansData) {
        if (plansData.isNotEmpty()) {
            selectedPlanData = mapLocalPlanToApiPlan(selectedPlan, plansData)
        }
    }

    val onPaymentClick = run {
        {
            if (selectedPlanData == null) return@run

            val isPendingPlan = pendingOrderPlan != null && selectedPlan == pendingOrderPlan
            if (isPendingPlan && pendingOrder != null) {
                val paymentUrl = pendingOrder?.paymentUrl
                if (!paymentUrl.isNullOrEmpty()) {
                    isPollingOrder = true
                    onUrlGenerated(paymentUrl)
                    return@run
                }
            }

            if (pendingOrderPlan != null && selectedPlan != pendingOrderPlan) {
                return@run
            }

            isLoading = true
            viewModel.viewModelScope.launch {
                try {
                    val planId = selectedPlanData?.plan ?: return@launch
                    val orderRequest = MemberOrderRequest(plan = planId)

                    val orderResponse = viewModel.createMemberOrder(orderRequest)
                    if (orderResponse.isSuccess && orderResponse.data != null) {
                        val order = orderResponse.data!!
                        currentOrderId = order.orderId
                        currentOrderStatus = MemberOrderStatus.fromString(order.status)
                        pendingOrder = order
                        pendingOrderPlan = selectedPlan
                        isPollingOrder = true
                        onUrlGenerated(order.paymentUrl!!)
                    } else {
                        isLoading = false
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to create order")
                    isLoading = false
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
            },
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            MemberUpgradeContent(selectedPlan = selectedPlan)
        }

        MemberUpgradePaymentButton(
            currentUserPlan = currentUserPlan,
            selectedPlan = selectedPlan,
            selectedPlanData = selectedPlanData,
            isLoading = isLoading,
            isLoadingPlans = isLoadingPlans,
            isCheckingPendingOrder = isCheckingPendingOrder,
            pendingOrderPlan = pendingOrderPlan,
            pendingOrder = pendingOrder,
            onPaymentClick = { onPaymentClick() }
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

@Preview
@Composable
private fun MixinMemberUpgradePagePreview() {
    MixinAppTheme {
        MixinMemberUpgradePage(Plan.ADVANCE, {}, {})
    }
}
