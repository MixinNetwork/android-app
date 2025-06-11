package one.mixin.android.ui.setting.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.Plan

@Composable
fun HeaderSection(plan: Plan) {
    val title = when (plan) {
        Plan.ADVANCE -> stringResource(id = R.string.membership_advance)
        Plan.ELITE -> stringResource(id = R.string.membership_elite)
        Plan.PROSPERITY -> stringResource(id = R.string.membership_prosperity)
        else -> ""
    }

    val content = when (plan) {
        Plan.ADVANCE -> stringResource(id = R.string.membership_advance_description)
        Plan.ELITE -> stringResource(id = R.string.membership_elite_description)
        Plan.PROSPERITY -> stringResource(id = R.string.membership_prosperity_description)
        else -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(
                backgroundColor = MixinAppTheme.colors.background,
                borderColor = MixinAppTheme.colors.borderColor
            )
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MembershipIcon(plan = plan, modifier = Modifier.size(70.dp))

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.W600
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textMinor,
            textAlign = TextAlign.Center
        )
    }
}
