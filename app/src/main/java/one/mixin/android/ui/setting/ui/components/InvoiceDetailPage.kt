package one.mixin.android.ui.setting.ui.components

import PageScaffold
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.setting.star.MemberInvoice
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.Plan

@Composable
fun InvoiceDetailPage(invoice: MemberInvoice, onPop: () -> Unit) {
    MixinAppTheme {
        PageScaffold(
            title = stringResource(R.string.Invoice),
            verticalScrollable = false,
            pop = onPop // 调用传入的 onPop 参数
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                HeaderSection(Plan.ADVANCE)
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
                        text = invoice.transactionId,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Plan",
                        color = MixinAppTheme.colors.textAssist
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = invoice.description,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Amount",
                        color = MixinAppTheme.colors.textAssist
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = invoice.amount,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Via",
                        color = MixinAppTheme.colors.textAssist
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = invoice.via,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Time",
                        color = MixinAppTheme.colors.textAssist
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = invoice.time,
                        color = MixinAppTheme.colors.textPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Status",
                        color = MixinAppTheme.colors.textAssist
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = invoice.status.name,
                        color = MixinAppTheme.colors.textPrimary
                    )
                }
            }
        }
    }
}
