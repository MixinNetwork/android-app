package one.mixin.android.ui.setting.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.MemberOrder
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.dayTime
import one.mixin.android.vo.MemberOrderStatus

@Composable
fun InvoicesList(
    invoices: List<MemberOrder>,
    onInvoiceClick: (MemberOrder) -> Unit,
    maxDisplayCount: Int? = null,
    onShowMoreClick: (() -> Unit)? = null,
) {
    val groupedInvoices = remember(invoices) {
        val displayedInvoices = maxDisplayCount?.let { invoices.take(it) } ?: invoices

        displayedInvoices
            .groupBy { order ->
                order.createdAt.dayTime()
            }
    }

    Column {
        if (invoices.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.no_invoices),
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textMinor
                )
            }
            return@Column
        }
        groupedInvoices.forEach { (dateStr, dateOrders) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    fontSize = 12.sp,
                    color = MixinAppTheme.colors.textAssist
                )
            }

            dateOrders.forEach { order ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 5.dp)
                        .clickable { onInvoiceClick(order) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(
                            when (order.after) {
                                "basic" -> R.drawable.ic_membership_advance
                                "standard" -> R.drawable.ic_membership_elite
                                else -> R.drawable.ic_membership_prosperity
                            }
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (order.type == "UPGRADE") {
                                "Upgrade to ${order.after}"
                            } else {
                                "Renew ${order.after} Plan"
                            },
                            fontSize = 14.sp,
                            color = MixinAppTheme.colors.textPrimary
                        )
                        Text(
                            text = order.status,
                            fontSize = 12.sp,
                            color = when (order.status.lowercase()) {
                                MemberOrderStatus.COMPLETED.value, MemberOrderStatus.PAID.value -> MixinAppTheme.colors.walletGreen
                                MemberOrderStatus.EXPIRED.value, MemberOrderStatus.FAILED.value -> MixinAppTheme.colors.walletRed
                                else -> MixinAppTheme.colors.textRemarks
                            }
                        )
                    }

                    if (order.stars >= 0) {
                        Text(
                            text = "+${order.stars} stars",
                            fontSize = 12.sp,
                            color = MixinAppTheme.colors.walletGreen
                        )
                    }
                }
            }
        }

        if (maxDisplayCount != null && invoices.size > maxDisplayCount) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable { onShowMoreClick?.invoke() },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.view_all),
                    color = MixinAppTheme.colors.accent,
                    fontSize = 14.sp
                )
            }
        }
    }
}
