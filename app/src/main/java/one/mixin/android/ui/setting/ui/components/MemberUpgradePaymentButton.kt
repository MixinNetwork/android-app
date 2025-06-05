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
import one.mixin.android.api.response.MemberOrderPlan
import one.mixin.android.ui.setting.ui.page.PlanPurchaseState
import one.mixin.android.ui.setting.ui.page.getPlanFromOrderAfter
import one.mixin.android.ui.setting.ui.page.mapLocalPlanToMemberOrderPlan

@Composable
fun MemberUpgradePaymentButton(
    currentUserPlan :Plan,
    selectedPlan :Plan,
    pendingOrder: MemberOrder?,
    purchaseState: PlanPurchaseState,
    onPaymentClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 30.dp)
    ) {
        if (selectedPlan != currentUserPlan) {
            Button(
                onClick = onPaymentClick,
                enabled = pendingOrder == null && purchaseState.isLoading.not(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (pendingOrder == null)
                        Color(0xFF3478F6) else Color.Gray
                )
            ) {
                if (purchaseState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 4.dp)
                    )
                }else if (pendingOrder!= null  && getPlanFromOrderAfter(pendingOrder.after) != selectedPlan){
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 4.dp)
                    )
                }else if (pendingOrder!= null ){
                    Text(
                        text = stringResource(id = R.string.Upgrading_Plan),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    val plan = mapLocalPlanToMemberOrderPlan(
                        selectedPlan,
                        purchaseState.availablePlans
                    )
                    Text(
                        text = plan?.let { "Upgrade for USD ${it.amountPayment}" } //todo currency
                            ?: stringResource(id = R.string.Upgrading_Plan),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
