package one.mixin.android.ui.setting.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.vo.Plan

@Composable
fun PlanSelector(
    selectedPlan: Plan,
    onPlanSelected: (Plan) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        listOf(Plan.ADVANCE, Plan.ELITE, Plan.PROSPERITY).forEachIndexed { index, plan ->
            if (index > 0) {
                Spacer(modifier = Modifier.width(12.dp))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .weight(1f)
                    .background(MixinAppTheme.colors.background)
                    .border(
                        width = 1.dp,
                        color = if (selectedPlan == plan) MixinAppTheme.colors.accent else MixinAppTheme.colors.borderColor,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable { onPlanSelected(plan) }
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(
                            id = when (plan) {
                                Plan.ADVANCE -> R.string.membership_advance
                                Plan.ELITE -> R.string.membership_elite
                                Plan.PROSPERITY -> R.string.membership_prosperity
                                else -> R.string.membership_advance
                            }
                        ),
                        color = if (selectedPlan == plan) MixinAppTheme.colors.accent else MixinAppTheme.colors.textAssist,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    MembershipIcon(
                        plan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}