package one.mixin.android.ui.home.web3.trade

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.widget.components.MixinButton

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
            .background(MixinAppTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                TradeTitle()
                TradeWalletSubtitle(walletId, walletName)
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(R.drawable.ic_circle_close),
                    contentDescription = stringResource(R.string.Close),
                    tint = Color.Unspecified,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TradeMoreGroup {
                TradeMoreRow(stringResource(R.string.Trade_Swap_Orders), swapCount) { onOrders("swap") }
                TradeMoreRow(stringResource(R.string.Trade_Limit_Orders), limitCount) { onOrders("limit") }
            }
            Spacer(Modifier.height(10.dp))
            TradeMoreGroup {
                TradeMoreRow(stringResource(R.string.Trade_Perpetual_Activity), activityCount, onActivity)
            }
            Spacer(Modifier.height(36.dp))
            TradeProPromo(onTradePro)
        }
    }
}

@Composable
private fun TradeMoreGroup(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor, cornerRadius = 13.dp),
    ) {
        content()
    }
}

@Composable
private fun TradeMoreRow(title: String, count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(start = 20.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 16.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            count.toString(),
            color = MixinAppTheme.colors.textRemarks,
            fontSize = 14.sp,
            lineHeight = 18.sp,
        )
        Icon(
            painterResource(R.drawable.ic_wallet_arrow_right),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(30.dp),
        )
    }
}

@Composable
private fun TradeProPromo(onTradePro: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .cardBackground(Color.Transparent, MixinAppTheme.colors.borderColor, cornerRadius = 13.dp)
            .padding(top = 30.dp, bottom = 21.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_trade_pro),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 38.dp)
                .aspectRatio(267f / 154f),
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.Trade_Pro_Title),
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(4.dp))
            TradeTierBadge(
                text = stringResource(R.string.Trade_Pro),
                color = MixinAppTheme.colors.accent,
                modifier = Modifier.background(Color(0xFFE8F4FF), RoundedCornerShape(4.dp)),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.Trade_Pro_Description),
            modifier = Modifier.padding(horizontal = 28.dp),
            color = MixinAppTheme.colors.textAssist,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(11.dp))
        MixinButton(
            onClick = onTradePro,
            shape = RoundedCornerShape(40.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            backgroundColor = Color(0xFF4B7CDD),
        ) {
            Text(
                stringResource(R.string.Trade_Now),
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 18.sp,
            )
        }
    }
}
