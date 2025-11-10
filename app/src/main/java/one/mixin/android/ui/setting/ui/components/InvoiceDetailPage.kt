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
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.MembershipOrder
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.timeFormat
import one.mixin.android.ui.viewmodel.MemberViewModel
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.MemberOrderStatus

@Composable
fun InvoiceDetailPage(orderId: String, onPop: () -> Unit, onCancel: (MembershipOrder) -> Unit) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<MemberViewModel>()
    val orderState = viewModel.getOrdersFlow(orderId).collectAsState(null)
    MixinAppTheme {
        PageScaffold(
            title = stringResource(R.string.Invoices),
            verticalScrollable = false,
            pop = onPop,
            actions = {
                IconButton(onClick = {
                    context.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_support),
                        contentDescription = null,
                        tint = MixinAppTheme.colors.icon,
                    )
                }
            }
        ) {
            val order = orderState.value ?: return@PageScaffold
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                InvoiceHeaderSection(order, onCancel)
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
                        text = stringResource(R.string.Transaction_Id).uppercase(),
                        color = MixinAppTheme.colors.textAssist,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = order.orderId,
                        color = MixinAppTheme.colors.textPrimary,
                        fontSize = 16.sp
                    )
                    if (order.category != "TRANS") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.membership_plan).uppercase(),
                            color = MixinAppTheme.colors.textAssist,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                color = MixinAppTheme.colors.textPrimary,
                                text = stringResource(
                                    when (order.after) {
                                        "basic" -> R.string.membership_advance
                                        "standard" -> R.string.membership_elite
                                        else -> R.string.membership_prosperity
                                    }
                                ),
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            MembershipIcon(
                                order.after,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.Amount).uppercase(),
                            color = MixinAppTheme.colors.textAssist,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "USD ${order.amount.numberFormat()}",
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.Description),
                        color = MixinAppTheme.colors.textAssist,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (order.category == "TRANS") {
                            stringResource(R.string.Rechange_Stars)
                        } else if (order.after == order.before) {
                            stringResource(R.string.invoice_renew_plan,
                                when (order.after) {
                                    "basic" -> stringResource(R.string.membership_advance)
                                    "standard" -> stringResource(R.string.membership_elite)
                                    else -> stringResource(R.string.membership_prosperity)
                                }
                            )
                        } else {
                            stringResource(R.string.invoice_upgrade_plan,
                                when (order.after) {
                                    "basic" -> stringResource(R.string.membership_advance)
                                    "standard" -> stringResource(R.string.membership_elite)
                                    else -> stringResource(R.string.membership_prosperity)
                                }
                            )
                        },
                        color = MixinAppTheme.colors.textPrimary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.Time),
                        color = MixinAppTheme.colors.textAssist,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = order.createdAt.timeFormat(),
                        color = MixinAppTheme.colors.textPrimary,
                        fontSize = 16.sp
                    )
                }
                if (order.category != "TRANS" && (order.status == MemberOrderStatus.COMPLETED.value || order.status == MemberOrderStatus.PAID.value)) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cardBackground(
                                MixinAppTheme.colors.background,
                                MixinAppTheme.colors.borderColor
                            )
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.Rewards),
                            color = MixinAppTheme.colors.textAssist,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_membership_star),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Mixin Star",
                                color = MixinAppTheme.colors.textPrimary,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Row(
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "+${order.stars}",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MixinAppTheme.colors.walletGreen
                                )
                                Text(
                                    text = " Stars",
                                    fontSize = 14.sp,
                                    color = MixinAppTheme.colors.textPrimary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
