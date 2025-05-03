package one.mixin.android.ui.setting.ui.page

import PageScaffold
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.session.Session
import one.mixin.android.ui.setting.LocalSettingNav
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.Membership
import one.mixin.android.vo.Plan
import one.mixin.android.vo.toUser

data class InvoiceItem(
    val id: String,
    val type: String,
    val date: String,
    val amount: String,
    val status: String,
    val statusColor: Color
)

@Composable
fun MixinStarInvoicesPage(pop: () -> Unit) {
    LocalSettingNav.current
    val membership = Session.getAccount()?.toUser()?.membership
    val sampleInvoices = listOf(
        InvoiceItem(
            id = "1",
            type = stringResource(R.string.Renew_Advance_Plan),
            date = "2023-06-15",
            amount = "$9.99",
            status = stringResource(R.string.Paid),
            statusColor = Color.Green
        ),
        InvoiceItem(
            id = "2",
            type = stringResource(R.string.Upgrade_to_Prosperity),
            date = "2023-05-12",
            amount = "$19.99",
            status = stringResource(R.string.Paid),
            statusColor = Color.Green
        )
    )
    MixinAppTheme {
        PageScaffold(
            title = stringResource(R.string.mixin_one),
            verticalScrollable = false,
            pop = pop
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .cardBackground(
                            MixinAppTheme.colors.background,
                            MixinAppTheme.colors.borderColor
                        )
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Text(stringResource(R.string.Plan), color = MixinAppTheme.colors.textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = membership?.plan?.value?.replaceFirstChar { it.uppercase() }
                                ?: "",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MixinAppTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(
                                id = membership?.memmembershipIcon()
                                    ?: R.drawable.ic_membership_advance
                            ),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MixinAppTheme.colors.background),
                            tint = MixinAppTheme.colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        onClick = {

                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = MixinAppTheme.colors.accent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.elevation(
                            pressedElevation = 0.dp,
                            defaultElevation = 0.dp,
                            hoveredElevation = 0.dp,
                            focusedElevation = 0.dp,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.View_Plan),
                            color = Color.White,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.cardBackground(
                        MixinAppTheme.colors.background,
                        MixinAppTheme.colors.borderColor
                    )
                ) {
                    InvoicesList(invoices = sampleInvoices)
                }
                Spacer(modifier = Modifier.width(10.dp))
            }
        }
    }
}

private fun Membership?.memmembershipIcon(): Int? = when {
    this == null -> View.NO_ID
    plan == Plan.ADVANCE -> R.drawable.ic_membership_advance
    plan == Plan.ELITE -> R.drawable.ic_membership_elite
    // PROSPERITY is animation icon
    else -> null
}

@Composable
private fun InvoiceItemRow(item: InvoiceItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(
                R.drawable.ic_membership_advance
            ),
            contentDescription = null,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MixinAppTheme.colors.background),
            tint = MixinAppTheme.colors.textPrimary
        )
        Text(
            modifier = Modifier.weight(1f),
            text = item.type,
            style = TextStyle(
                fontWeight = FontWeight.W500,
                fontSize = 16.sp,
                color = MixinAppTheme.colors.textPrimary
            )
        )

        Text(
            text = item.amount,
            style = TextStyle(
                fontWeight = FontWeight.W500,
                fontSize = 16.sp,
                color = MixinAppTheme.colors.textPrimary
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.status,
            style = TextStyle(
                fontWeight = FontWeight.W400,
                fontSize = 14.sp,
                color = item.statusColor
            )
        )
    }
}

@Composable
private fun InvoicesList(invoices: List<InvoiceItem>) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.Invoices),
                fontWeight = FontWeight.W500,
                color = MixinAppTheme.colors.textPrimary
            )
        }

        if (invoices.isEmpty()) {
            Text(
                text = stringResource(R.string.No_Invoices),
                color = MixinAppTheme.colors.textAssist,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            invoices.forEachIndexed { index, item ->
                InvoiceItemRow(item = item)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.view_all),
                color = MixinAppTheme.colors.textPrimary,
            )
        }
    }
}

@Preview
@Composable
private fun MixinStarInvoicesPagePreview() {
    MixinStarInvoicesPage {

    }
}
