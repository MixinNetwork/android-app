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
    val (title, icon) = when (order.after) {
        "basic" -> Pair("Advance Plan", R.drawable.ic_membership_advance)
        "standard" -> Pair("Elite Plan", R.drawable.ic_membership_elite)
        else -> Pair("Prosperity Plan", R.drawable.ic_membership_prosperity)
    }

    val statusColor = if (order.status == MemberOrderStatus.EXPIRED.value || order.status == MemberOrderStatus.FAILED.value || order.status == MemberOrderStatus.CANCEL.value) {
        MixinAppTheme.colors.walletRed
    } else {
        MixinAppTheme.colors.walletGreen
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
            text = order.status,
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
