package one.mixin.android.ui.setting.ui.page

import PageScaffold
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.MembershipOrder
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.setting.ui.components.InvoicesList
import one.mixin.android.ui.viewmodel.MemberViewModel
import one.mixin.android.ui.wallet.alert.components.cardBackground

@Composable
fun AllInvoicesPage(
    onPop: () -> Unit,
    onOrderClick: (MembershipOrder) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<MemberViewModel>()
    val orders = viewModel.getAllMemberOrders().collectAsState(initial = emptyList())
    MixinAppTheme {
    PageScaffold (
        title = "All Invoices",
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
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .cardBackground(
                    MixinAppTheme.colors.background,
                    MixinAppTheme.colors.borderColor
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row ( modifier = Modifier
                .padding(vertical = 10.dp)){
            Text(stringResource(R.string.Invoices), color = MixinAppTheme.colors.textMinor, fontSize = 14.sp)}

            InvoicesList(
                invoices = orders.value,
                onInvoiceClick = onOrderClick
            )
        }
    }}
}
