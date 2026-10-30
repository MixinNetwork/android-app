package one.mixin.android.ui.home.web3.trade

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun TradeTitle() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.Trade),
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.Trade_Lite),
            color = MixinAppTheme.colors.textAssist,
            fontSize = 12.sp,
            modifier = Modifier
                .border(1.dp, MixinAppTheme.colors.borderGray, RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp),
        )
    }
}

@Composable
fun TradeWalletSubtitle(walletId: String?, walletName: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (walletId == null) {
                stringResource(R.string.Privacy_Wallet)
            } else {
                walletName ?: stringResource(R.string.Common_Wallet)
            },
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = MixinAppTheme.colors.textAssist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (walletId == null) {
            Spacer(Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.ic_wallet_privacy),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}
