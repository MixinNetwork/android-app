package one.mixin.android.ui.home.web3.trade

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.home.web3.components.ActionButton

@Composable
fun TradeMoreBottomSheet(
    walletId: String?,
    walletName: String?,
    swapCount: Int,
    limitCount: Int,
    activityCount: Int,
    onOrders: (String) -> Unit,
    onActivity: () -> Unit,
    onTradePro: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MixinAppTheme.colors.backgroundWindow)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 32.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                TradeTitle()
                TradeWalletSubtitle(walletId, walletName)
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(R.drawable.ic_circle_close),
                    contentDescription = stringResource(R.string.Close),
                    tint = Color.Unspecified,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Column(
            Modifier.clip(RoundedCornerShape(12.dp))
                .background(MixinAppTheme.colors.background),
        ) {
            TradeMoreRow(stringResource(R.string.Trade_Swap_Orders), swapCount) { onOrders("swap") }
            TradeMoreRow(stringResource(R.string.Trade_Limit_Orders), limitCount) { onOrders("limit") }
        }
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier.clip(RoundedCornerShape(12.dp))
                .background(MixinAppTheme.colors.background),
        ) {
            TradeMoreRow(stringResource(R.string.Trade_Perpetual_Activity), activityCount, onActivity)
        }
        Spacer(Modifier.height(36.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MixinAppTheme.colors.background)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_trade_pro),
                contentDescription = null,
                modifier = Modifier.widthIn(max = 267.dp).fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.Trade_Pro_Title),
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.Trade_Pro),
                    color = MixinAppTheme.colors.accent,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(MixinAppTheme.colors.accent.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.Trade_Pro_Description),
                color = MixinAppTheme.colors.textAssist,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            ActionButton(
                text = stringResource(R.string.Trade_Now),
                onClick = onTradePro,
                backgroundColor = MixinAppTheme.colors.accent,
                contentColor = Color.White,
            )
        }
    }
}

@Composable
private fun TradeMoreRow(title: String, count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 16.sp,
        )
        Spacer(Modifier.width(8.dp))
        Text(count.toString(), color = MixinAppTheme.colors.textAssist, fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Icon(
            painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = MixinAppTheme.colors.textAssist,
            modifier = Modifier.size(16.dp),
        )
    }
}
