package one.mixin.android.ui.setting.ui.page

import PageScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.MembershipOrder
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.setting.ui.components.InvoicesList
import one.mixin.android.ui.setting.ui.components.MembershipPlanCard
import one.mixin.android.ui.viewmodel.MemberViewModel
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.Membership

@Composable
fun MixinMemberInvoicesPage(
    membership: Membership,
    onPop: () -> Unit,
    onViewPlanClick: () -> Unit,
    onAll: () -> Unit,
    onOrderClick: (MembershipOrder) -> Unit
    onReferral:() -> Unit
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<MemberViewModel>()
    val orders = viewModel.getAllMemberOrders().collectAsState(emptyList())
    MixinAppTheme {
        PageScaffold(
            title = stringResource(R.string.mixin_one),
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
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFF2E8FF),
                                    Color(0xFFE6F1FF)
                                )
                            )
                        )
                        .padding(16.dp)
                        .clickable {
                            onReferral.invoke()
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_referral),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.referral_desc),
                        color = Color(red = 0xAA, green = 0x71, blue = 0xFA),
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_referral_arrow),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                MembershipPlanCard(
                    membership = membership,
                    onViewPlanClick = onViewPlanClick
                )

                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .cardBackground(
                            MixinAppTheme.colors.background,
                            MixinAppTheme.colors.borderColor
                        )
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Row(modifier = Modifier.clickable{
                        onAll.invoke()
                    },
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.Invoices), color = MixinAppTheme.colors.textMinor, fontSize = 14.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_gray_right),
                            contentDescription = null,
                            tint = Color.Unspecified
                        )
                    }
                    InvoicesList(
                        invoices = orders.value,
                        onInvoiceClick = { order ->
                            onOrderClick(order)
                        },
                        maxDisplayCount = 10, {
                            onAll.invoke()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
