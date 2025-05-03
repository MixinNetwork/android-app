package one.mixin.android.ui.setting.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
    val (title, content, icon) = when (plan) {
        Plan.ADVANCE -> Triple(
            stringResource(id = R.string.plan_advance),
            stringResource(id = R.string.plan_advance_content),
            R.drawable.ic_membership_advance
        )
        Plan.ELITE -> Triple(
            stringResource(id = R.string.plan_elite),
            stringResource(id = R.string.plan_elite_content),
            R.drawable.ic_membership_elite
        )
        Plan.PROSPERITY -> Triple(
            stringResource(id = R.string.plan_prosperity),
            stringResource(id = R.string.plan_prosperity_content),
            R.drawable.ic_membership_prosperity
        )
        else -> throw IllegalStateException()
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
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(70.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
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
