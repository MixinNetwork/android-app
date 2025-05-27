package one.mixin.android.ui.setting.ui.page

import PageScaffold
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.setting.member.MemberInvoice
import one.mixin.android.ui.setting.ui.components.InvoicesList
import one.mixin.android.ui.setting.ui.components.MembershipPlanCard
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.Membership

@Composable
fun MixinStarInvoicesPage(
    membership: Membership,
    invoices: List<MemberInvoice>,
    onPop: () -> Unit,
    onViewPlanClick: () -> Unit,
    onAll: () -> Unit,
    onInvoiceClick: (MemberInvoice) -> Unit
) {
    MixinAppTheme {
        PageScaffold(
            title = stringResource(R.string.mixin_one),
            verticalScrollable = false,
            pop = onPop
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                MembershipPlanCard(
                    membership = membership,
                    onViewPlanClick = onViewPlanClick
                )

                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.cardBackground(
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
                        invoices = invoices,
                        onInvoiceClick = { invoice ->
                            onInvoiceClick(invoice)
                        },
                        maxDisplayCount = 10
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }
        }
    }
}
