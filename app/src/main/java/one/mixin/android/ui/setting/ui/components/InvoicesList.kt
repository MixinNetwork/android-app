package one.mixin.android.ui.setting.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.setting.member.InvoiceType
import one.mixin.android.ui.setting.member.MemberInvoice
import one.mixin.android.vo.Plan

@Composable
fun InvoicesList(
    invoices: List<MemberInvoice>,
    onInvoiceClick: (MemberInvoice) -> Unit,
    maxDisplayCount: Int? = null
) {
    val displayedInvoices = maxDisplayCount?.let { invoices.take(it) } ?: invoices

    Column {
        displayedInvoices.forEach { invoice ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable { onInvoiceClick(invoice) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(
                        when (invoice.plan) {
                            Plan.ADVANCE -> R.drawable.ic_membership_advance
                            Plan.ELITE -> R.drawable.ic_membership_elite
                            else -> R.drawable.ic_membership_prosperity
                        }
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (invoice.type == InvoiceType.UPGRADE) {
                            "Upgrade to ${invoice.description}"
                        } else {
                            "Renew ${invoice.description}"
                        },
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textPrimary
                    )
                }
                if (invoice.type == InvoiceType.PURCHASE) {
                    Text(
                        text = "+2 stars",
                        fontSize = 12.sp,
                        color = MixinAppTheme.colors.walletGreen
                    )
                }
            }
        }

        if (maxDisplayCount != null && invoices.size > maxDisplayCount) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "View All",
                    color = MixinAppTheme.colors.accent,
                    fontSize = 14.sp
                )
            }
        }
    }
}
