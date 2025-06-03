package one.mixin.android.ui.setting.ui.components

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.MemberOrder
import one.mixin.android.vo.Plan
import timber.log.Timber
import one.mixin.android.api.response.Plan as ApiPlan

@Composable
fun MemberUpgradePaymentButton(
    currentUserPlan: Plan,
    selectedPlan: Plan,
    selectedPlanData: ApiPlan?,
    isLoading: Boolean,
    isLoadingPlans: Boolean,
    isCheckingPendingOrder: Boolean,
    pendingOrderPlan: Plan?,
    pendingOrder: MemberOrder?,
    onPaymentClick: () -> Unit
) {
    val isPendingPlan = pendingOrderPlan != null && selectedPlan == pendingOrderPlan

    val shouldShowButton = when (currentUserPlan) {
        Plan.ADVANCE -> selectedPlan == Plan.ELITE || selectedPlan == Plan.PROSPERITY
        Plan.ELITE -> selectedPlan == Plan.PROSPERITY
        Plan.PROSPERITY -> false
        else -> true
    }
    Timber.e("$selectedPlan $pendingOrderPlan, $isLoading, $isLoadingPlans, $isCheckingPendingOrder, $pendingOrder")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 30.dp)
    ) {
        if (shouldShowButton) {
            Button(
                onClick = onPaymentClick,
                enabled = (!isLoading && !isLoadingPlans && !isCheckingPendingOrder && selectedPlanData != null) &&
                    (pendingOrderPlan == null || selectedPlan == pendingOrderPlan),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (pendingOrderPlan == null || selectedPlan == pendingOrderPlan)
                        Color(0xFF3478F6) else Color.Gray
                )
            ) {
                if (isLoading || isLoadingPlans || isCheckingPendingOrder || (pendingOrderPlan != null && selectedPlan == pendingOrderPlan)) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 4.dp)
                    )
                } else {
                    val priceText = if (isPendingPlan && pendingOrder != null) {
                        "Upgrade for USD ${pendingOrder.amount}"
                    } else if (pendingOrderPlan != null) {
                        stringResource(id = R.string.upgrading)
                    } else {
                        selectedPlanData?.let { "Upgrade for USD ${it.amountPayment}" } ?: stringResource(id = R.string.upgrading)
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
    }
}
