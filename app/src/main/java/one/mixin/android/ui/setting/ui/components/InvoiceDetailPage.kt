package one.mixin.android.ui.setting.ui.components

import PageScaffold
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import one.mixin.android.R
import one.mixin.android.api.response.MemberOrder
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.setting.member.getInvoiceStatus
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.MemberOrderStatus

@Composable
fun InvoiceDetailPage(order: MemberOrder, onPop: () -> Unit) {
    MixinAppTheme {
        PageScaffold(
            title = stringResource(R.string.Invoice),
            verticalScrollable = false,
            pop = onPop
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                InvoiceHeaderSection(order)
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .cardBackground(
                            MixinAppTheme.colors.background,
                            MixinAppTheme.colors.borderColor
                        )
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = "Transaction ID",
                        color = MixinAppTheme.colors.textAssist
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = order.orderId,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Plan",
                        color = MixinAppTheme.colors.textAssist
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = order.after,
                            color = MixinAppTheme.colors.textPrimary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        MembershipIcon(
                            order.after,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Amount",
                        color = MixinAppTheme.colors.textAssist
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = order.amount,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Via",
                        color = MixinAppTheme.colors.textAssist
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = order.method,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Time",
                        color = MixinAppTheme.colors.textAssist
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = order.createdAt.toString(),
                        color = MixinAppTheme.colors.textPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                if (order.status == MemberOrderStatus.COMPLETED.value || order.status == MemberOrderStatus.PAID.value) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cardBackground(
                                MixinAppTheme.colors.background,
                                MixinAppTheme.colors.borderColor
                            )
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                    ) {
                        Text(
                            text = "Rewards",
                            color = MixinAppTheme.colors.textAssist
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_member_star),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Mixin Star",
                                color = MixinAppTheme.colors.textAssist
                            )
                            Text(
                                text = "+${order.stars} stars",
                                color = MixinAppTheme.colors.textAssist
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
