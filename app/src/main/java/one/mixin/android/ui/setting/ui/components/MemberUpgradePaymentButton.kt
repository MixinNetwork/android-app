package one.mixin.android.ui.setting.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.BuildConfig
import one.mixin.android.R
import one.mixin.android.api.response.MembershipOrder
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.setting.ui.page.PlanPurchaseState
import one.mixin.android.ui.setting.ui.page.getPlanFromOrderAfter
import one.mixin.android.ui.setting.ui.page.isPlanAvailableInGooglePlay
import one.mixin.android.ui.setting.ui.page.mapLocalPlanToMemberOrderPlan
import one.mixin.android.ui.viewmodel.MemberViewModel
import one.mixin.android.vo.Plan

@Composable
fun MemberUpgradePaymentButton(
    currentUserPlan: Plan,
    selectedPlan: Plan,
    pendingOrder: MembershipOrder?,
    purchaseState: PlanPurchaseState,
    savedOrderId: String? = null,
    onPaymentClick: () -> Unit,
    onContactSupport: () -> Unit,
    onViewInvoice: (MembershipOrder) -> Unit = {}
) {
    val viewModel: MemberViewModel = hiltViewModel()
    val subscriptionPlans by viewModel.subscriptionPlans.collectAsState()
    if (selectedPlan == currentUserPlan) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 30.dp)
        ) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                onClick = {},
                enabled = false,
                colors = ButtonDefaults.buttonColors(backgroundColor = MixinAppTheme.colors.backgroundGray),
            ) {
                Text(
                    text = stringResource(id = R.string.Current_Plan),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }}
    } else if (selectedPlan.ordinal < currentUserPlan.ordinal) {
        Spacer(modifier = Modifier.height(24.dp))
        return
    } else if (selectedPlan != currentUserPlan) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 30.dp)
        ) {
            val isPlanAvailable = isPlanAvailableInGooglePlay(
                selectedPlan,
                purchaseState.availablePlans,
                purchaseState.availablePlayStorePlans
            )

            val plan = mapLocalPlanToMemberOrderPlan(selectedPlan, purchaseState.availablePlans)

            val subscriptionPlanInfo =
                if (BuildConfig.IS_GOOGLE_PLAY && plan?.playStoreSubscriptionId != null) {
                    subscriptionPlans.find { it.planId == plan.playStoreSubscriptionId }
                } else {
                    null
                }

            val isGooglePlayUnavailable = BuildConfig.IS_GOOGLE_PLAY && !isPlanAvailable

            Button(
                onClick = {
                    if (selectedPlan == Plan.PROSPERITY) {
                        onContactSupport()
                    } else if (savedOrderId == null && pendingOrder != null && selectedPlan == getPlanFromOrderAfter(pendingOrder.after)) {
                        onViewInvoice(pendingOrder)
                    } else {
                        onPaymentClick()
                    }
                },
                enabled = selectedPlan == Plan.PROSPERITY ||
                    (savedOrderId == null && pendingOrder != null && selectedPlan == getPlanFromOrderAfter(pendingOrder.after)) ||
                    (pendingOrder == null &&
                    purchaseState.isLoading.not() &&
                    !isGooglePlayUnavailable),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = when {
                        selectedPlan == Plan.PROSPERITY -> MixinAppTheme.colors.accent
                        savedOrderId == null && pendingOrder != null && selectedPlan == getPlanFromOrderAfter(pendingOrder.after) -> MixinAppTheme.colors.accent
                        pendingOrder == null -> MixinAppTheme.colors.accent
                        else -> MixinAppTheme.colors.backgroundGray
                    }
                )
            ) {
                when {
                    savedOrderId == null && pendingOrder != null && selectedPlan == getPlanFromOrderAfter(pendingOrder.after) -> {
                        Text(
                            text = stringResource(id = R.string.View_Invoice),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    selectedPlan == Plan.PROSPERITY -> {
                        Text(
                            text = stringResource(id = R.string.Contact_Support),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    purchaseState.isLoading -> {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 4.dp)
                        )
                    }

                    pendingOrder != null && getPlanFromOrderAfter(pendingOrder.after) != selectedPlan -> {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 4.dp)
                        )
                    }

                    pendingOrder != null -> {
                        Text(
                            text = stringResource(id = R.string.Upgrading_Plan),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    isGooglePlayUnavailable -> {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .size(20.dp)
                        )
                    }

                    subscriptionPlanInfo != null -> {
                        val basePhase = subscriptionPlanInfo.pricingPhaseList.firstOrNull()
                        if (basePhase != null) {
                            Text(
                                text = stringResource(
                                    R.string.Upgrade_Plan_for,
                                    "${basePhase.priceCurrencyCode} ${basePhase.getPriceAmount()}"
                                ),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = if (plan != null) {
                                    stringResource(
                                        R.string.Upgrade_Plan_for,
                                        "USD ${plan.amountPayment}"
                                    )
                                } else {
                                    stringResource(id = R.string.Upgrading_Plan)
                                },
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    else -> {
                        val priceText = if (plan != null) {
                            stringResource(R.string.Upgrade_Plan_for, "USD ${plan.amountPayment}")
                        } else {
                            stringResource(id = R.string.Upgrading_Plan)
                        }
                        Text(
                            text = priceText,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            if (savedOrderId == null && pendingOrder != null && selectedPlan == getPlanFromOrderAfter(pendingOrder.after)) {
                Text(
                    text = stringResource(id = R.string.verifying_payment),
                    color = MixinAppTheme.colors.textAssist,
                )
            }
        }
    } else {
        Spacer(modifier = Modifier.height(24.dp))
    }
}
