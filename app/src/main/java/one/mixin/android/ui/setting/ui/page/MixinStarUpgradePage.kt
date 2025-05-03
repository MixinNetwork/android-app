package one.mixin.android.ui.setting.ui.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.setting.LocalSettingNav
import one.mixin.android.ui.setting.ui.components.HeaderSection
import one.mixin.android.ui.setting.ui.components.MemberSection
import one.mixin.android.ui.setting.ui.components.PlanSelector
import one.mixin.android.ui.setting.ui.components.ProfileSection
import one.mixin.android.vo.Plan

@Composable
fun MixinStarUpgradePage() {
    LocalSettingNav.current
    var selectedPlan by remember { mutableStateOf(Plan.ADVANCE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(id = R.string.mixin_one),
                fontSize = 18.sp,
                fontWeight = FontWeight.W700,
                color = MixinAppTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                // Todo
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_circle_close),
                    tint = Color.Unspecified,
                    contentDescription = stringResource(id = R.string.close)
                )
            }
        }

        PlanSelector(
            selectedPlan = selectedPlan,
            onPlanSelected = { selectedPlan = it }
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {

            HeaderSection(plan = selectedPlan)
            Spacer(modifier = Modifier.height(10.dp))
            ProfileSection(plan = selectedPlan)
            Spacer(modifier = Modifier.height(10.dp))
            MemberSection(plan = selectedPlan)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)) {
            Button(
                onClick = { /* Handle upgrade action */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF3478F6))
            ) {
                Text(
                    text = stringResource(id = R.string.upgrade_price),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Preview
@Composable
private fun MixinStarUpgradePagePreview() {
    MixinAppTheme {
        MixinStarUpgradePage()
    }
}
