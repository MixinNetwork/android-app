package one.mixin.android.ui.setting.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import one.mixin.android.api.response.MemberOrder
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.MemberOrderStatus

@Composable
fun InvoiceHeaderSection(order: MemberOrder) {
    val title = when (order.after) {
        "basic" -> stringResource(R.string.membership_advance)
        "standard" -> stringResource(R.string.membership_elite)
        else -> stringResource(R.string.membership_prosperity)
    }

    val statusColor = when (order.status.lowercase()) {
            MemberOrderStatus.COMPLETED.value, MemberOrderStatus.PAID.value -> MixinAppTheme.colors.walletGreen
            MemberOrderStatus.EXPIRED.value, MemberOrderStatus.FAILED.value -> MixinAppTheme.colors.walletRed
            else -> MixinAppTheme.colors.textRemarks
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
        MembershipIcon(
            order.after,
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
            text = when(order.status) {
                MemberOrderStatus.COMPLETED.value -> stringResource(R.string.Completed)
                MemberOrderStatus.PAID.value -> stringResource(R.string.Paid)
                MemberOrderStatus.EXPIRED.value -> stringResource(R.string.Expired)
                MemberOrderStatus.FAILED.value -> stringResource(R.string.Failed)
                MemberOrderStatus.INITIAL.value -> stringResource(R.string.Pending)
                else -> stringResource(R.string.Unknown)
            },
            fontSize = 14.sp,
            color = statusColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 2.5.dp)
        )
    }
}
