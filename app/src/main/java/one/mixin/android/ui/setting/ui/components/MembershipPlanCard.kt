package one.mixin.android.ui.setting.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import one.mixin.android.vo.Membership
import one.mixin.android.vo.Plan
import org.threeten.bp.Instant

@Composable
fun MembershipPlanCard(
    membership: Membership,
    onViewPlanClick: () -> Unit
) {
    val isExpired = membership.expiredAt.let {
        Instant.now().isAfter(Instant.parse(it))
    }

    Column(
        modifier = Modifier
            .cardBackground(
                MixinAppTheme.colors.background,
                MixinAppTheme.colors.borderColor
            )
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
            onViewPlanClick.invoke()
        }) {
            Text(
                stringResource(R.string.membership_plan),
                color = MixinAppTheme.colors.textMinor,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(R.drawable.ic_arrow_gray_right),
                contentDescription = null,
                tint = Color.Unspecified
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (membership.plan) {
                    Plan.ADVANCE -> stringResource(R.string.membership_advance)
                    Plan.ELITE -> stringResource(R.string.membership_elite)
                    else -> stringResource(R.string.membership_prosperity)
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MixinAppTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.width(4.dp))
            MembershipIcon(
                membership.plan,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MixinAppTheme.colors.background),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(
                R.string.expires_on,
                membership.expiredAt.substringBefore("T") ?: "Unknown"
            ),
            color = if (isExpired) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.textMinor,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            onClick = onViewPlanClick,
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = MixinAppTheme.colors.accent
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.elevation(
                pressedElevation = 0.dp,
                defaultElevation = 0.dp,
                hoveredElevation = 0.dp,
                focusedElevation = 0.dp,
            ),
        ) {
            Text(
                text = stringResource(R.string.View_Plan),
                color = Color.White,
            )
        }
    }
}
