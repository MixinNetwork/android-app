package one.mixin.android.ui.home.web3.trade.perps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences

@Composable
internal fun PerpsConfirmationMarket(
    iconUrl: String?,
    symbol: String,
    leverage: Int,
    isLong: Boolean,
    isPending: Boolean = false,
) {
    val reversed = LocalContext.current.defaultSharedPreferences.getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val sideColor = if (isLong != reversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    val labelColor = if (isPending) MixinAppTheme.colors.textAssist else sideColor
    val labelBackground = if (isPending) MixinAppTheme.colors.backgroundGrayLight else sideColor.copy(alpha = 0.1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        CoilImage(
            model = iconUrl,
            placeholder = R.drawable.ic_avatar_place_holder,
            modifier = Modifier.size(24.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = symbol,
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "${leverage}x",
            color = labelColor,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(labelBackground).padding(horizontal = 3.dp, vertical = 1.dp),
        )
    }
}
