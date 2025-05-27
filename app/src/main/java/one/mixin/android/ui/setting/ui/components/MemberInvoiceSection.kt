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
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.setting.member.InvoiceStatus
import one.mixin.android.ui.setting.member.MemberInvoice

@Composable
fun MemberInvoiceSection(invoice: MemberInvoice) {
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
                id = R.drawable.ic_membership_advance
            ),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(70.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = invoice.description,
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${invoice.amount} via ${invoice.via}",
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textMinor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = invoice.time,
            fontSize = 12.sp,
            color = MixinAppTheme.colors.textMinor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        StatusBadge(status = invoice.status)
    }
}

@Composable
fun StatusBadge(status: InvoiceStatus) {
    val (text, backgroundColor) = when (status) {
        InvoiceStatus.EXPIRED -> "Expired" to Color.Red.copy(alpha = 0.1f)
        InvoiceStatus.COMPLETED -> "Completed" to Color.Green.copy(alpha = 0.1f)
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
            color = if (status == InvoiceStatus.EXPIRED) Color.Red else Color.Green,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
