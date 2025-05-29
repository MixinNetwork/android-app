package one.mixin.android.ui.setting.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

@Composable
fun MemberInvoiceSection(order: MemberOrder) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MixinAppTheme.colors.background,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(
                id = when (order.after) {
                    "ADVANCE" -> R.drawable.ic_membership_advance
                    "ELITE" -> R.drawable.ic_membership_elite
                    else -> R.drawable.ic_membership_prosperity
                }
            ),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(70.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = order.after,
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${order.amount} via ${order.method}",
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textMinor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = order.createdAt,
            fontSize = 12.sp,
            color = MixinAppTheme.colors.textMinor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        StatusBadge(status = order.status)
    }
}

@Composable
fun StatusBadge(status: String) {
    val (text, backgroundColor) = when (status) {
        "EXPIRED" -> "Expired" to Color.Red.copy(alpha = 0.1f)
        "COMPLETED" -> "Completed" to Color.Green.copy(alpha = 0.1f)
        else -> "Pending" to Color.Yellow.copy(alpha = 0.1f)
    }

    Row(
        modifier = Modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            color = when (status) {
                "EXPIRED" -> Color.Red
                "COMPLETED" -> Color.Green
                else -> Color(0xFFCC9900)
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
