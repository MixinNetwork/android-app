package one.mixin.android.ui.setting.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import one.mixin.android.vo.Plan

@Composable
fun MemberUpgradeContent(
    selectedPlan: Plan
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        HeaderSection(plan = selectedPlan)
        Spacer(modifier = Modifier.height(10.dp))
        ProfileSection(plan = selectedPlan)
        Spacer(modifier = Modifier.height(10.dp))
        MemberSection(plan = selectedPlan)
    }
}
