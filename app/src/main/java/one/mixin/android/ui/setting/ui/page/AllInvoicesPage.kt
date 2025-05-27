package one.mixin.android.ui.setting.ui.page

import PageScaffold
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.setting.member.MemberInvoice
import one.mixin.android.ui.setting.ui.components.InvoicesList
import one.mixin.android.ui.wallet.alert.components.cardBackground

@Composable
fun AllInvoicesPage(invoices: List<MemberInvoice>, onPop: () -> Unit) {
    PageScaffold (
        title = "All Invoices",
        verticalScrollable = false,
        pop = onPop
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .cardBackground(
                    MixinAppTheme.colors.background,
                    MixinAppTheme.colors.borderColor
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            InvoicesList(
                invoices = invoices,
                onInvoiceClick = { invoice ->
                    // Handle invoice click, navigate to detail page
                }
            )
        }
    }
}
