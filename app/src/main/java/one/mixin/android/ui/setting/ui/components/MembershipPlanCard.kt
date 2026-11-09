package one.mixin.android.ui.setting.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import org.threeten.bp.Duration
import org.threeten.bp.Instant

private val RENEW_REMINDER_DURATION = Duration.ofDays(14)

@Composable
fun MembershipPlanCard(
    membership: Membership,
    onViewPlanClick: () -> Unit,
    onRenewPlanClick: () -> Unit,
) {
    val expiredAt = remember(membership.expiredAt) {
        runCatching { Instant.parse(membership.expiredAt) }.getOrNull()
    }
    val now = Instant.now()
    val isExpired = expiredAt != null && now.isAfter(expiredAt)
    val isExpiringSoon = expiredAt != null && !isExpired && membership.plan != Plan.None &&
        Duration.between(now, expiredAt) <= RENEW_REMINDER_DURATION
    val showRenew = isExpired || isExpiringSoon
    val showViewPlan = !isExpired
    val highlightExpiry = expiredAt != null && Duration.between(now, expiredAt) <= Duration.ofDays(145)

    Column(
        modifier = Modifier
            .cardBackground(
                MixinAppTheme.colors.background,
                MixinAppTheme.colors.borderColor
            )
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.membership_plan),
                color = MixinAppTheme.colors.textMinor,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (membership.plan) {
                    Plan.ADVANCE -> stringResource(R.string.membership_advance)
                    Plan.ELITE -> stringResource(R.string.membership_elite)
                    Plan.PROSPERITY -> stringResource(R.string.membership_prosperity)
                    Plan.None -> stringResource(R.string.None)
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
        if (membership.expiredAt != "0000-00-00T00:00:00.0000Z") {
            Text(
                text = stringResource(
                    if (isExpired) R.string.expired_on else R.string.expires_on,
                    membership.expiredAt.substringBefore("T")
                ),
                color = if (highlightExpiry) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.textMinor,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        if (showRenew) {
            PlanCardButton(
                text = stringResource(R.string.Renew_Plan),
                primary = true,
                onClick = onRenewPlanClick,
            )
        }
        if (showRenew && showViewPlan) {
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (showViewPlan) {
            PlanCardButton(
                text = stringResource(R.string.View_Plan),
                primary = !showRenew,
                onClick = onViewPlanClick,
            )
        }
    }
}

@Composable
private fun PlanCardButton(
    text: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = if (primary) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundWindow
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
            text = text,
            color = if (primary) Color.White else MixinAppTheme.colors.accent,
        )
    }
}
