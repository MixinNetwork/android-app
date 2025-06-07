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
import androidx.compose.ui.text.font.FontWeight
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
                    text = stringResource(R.string.No_Invoices),
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
                    if (order.category != "TRANS") {
                        MembershipIcon(
                            order.after,
                            modifier = Modifier.size(32.dp),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_membership_star),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (order.category == "TRANS") stringResource(R.string.Buy_Stars) else if (order.after == order.before) {
                                stringResource(
                                    R.string.invoice_renew_plan,
                                    when (order.after) {
                                        "basic" -> stringResource(R.string.membership_advance)
                                        "standard" -> stringResource(R.string.membership_elite)
                                        else -> stringResource(R.string.membership_prosperity)
                                    }
                                )
                            } else {
                                stringResource(
                                    R.string.invoice_upgrade_plan,
                                    when (order.after) {
                                        "basic" -> stringResource(R.string.membership_advance)
                                        "standard" -> stringResource(R.string.membership_elite)
                                        else -> stringResource(R.string.membership_prosperity)
                                    }
                                )
                            },
                            fontSize = 14.sp,
                            color = MixinAppTheme.colors.textPrimary
                        )
                        Text(
                            text = when (order.status) {
                                MemberOrderStatus.COMPLETED.value -> stringResource(R.string.Completed)
                                MemberOrderStatus.PAID.value -> stringResource(R.string.Paid)
                                MemberOrderStatus.EXPIRED.value -> stringResource(R.string.Expired)
                                MemberOrderStatus.FAILED.value -> stringResource(R.string.Failed)
                                MemberOrderStatus.REFUNDED.value -> stringResource(R.string.Refunded)
                                MemberOrderStatus.INITIAL.value -> stringResource(R.string.Pending)
                                MemberOrderStatus.CANCEL.value -> stringResource(R.string.Canceled)
                                else -> stringResource(R.string.Unknown)
                            },
                            fontSize = 12.sp,
                            color = when (order.status.lowercase()) {
                                MemberOrderStatus.COMPLETED.value, MemberOrderStatus.PAID.value -> MixinAppTheme.colors.walletGreen
                                MemberOrderStatus.CANCEL.value, MemberOrderStatus.REFUNDED.value, MemberOrderStatus.EXPIRED.value, MemberOrderStatus.FAILED.value -> MixinAppTheme.colors.walletRed
                                else -> MixinAppTheme.colors.textRemarks
                            }
                        )
                    }
                    if (order.stars >= 0 && (order.status == MemberOrderStatus.COMPLETED.value || order.status == MemberOrderStatus.PAID.value)) {
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "+${order.stars}",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.W500,
                                color = MixinAppTheme.colors.walletGreen
                            )
                            Text(
                                text = " stars",
                                fontSize = 14.sp,
                                color = MixinAppTheme.colors.textPrimary
                            )
                        }
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
