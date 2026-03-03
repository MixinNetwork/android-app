package one.mixin.android.ui.home.web3.trade

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsPositionHistoryItem
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.ui.wallet.alert.components.cardBackground
import java.math.BigDecimal

@Composable
fun ClosedPositionItem(
    position: PerpsPositionHistoryItem,
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val quoteColorPref = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    
    val pnl = try {
        BigDecimal(position.realizedPnl)
    } catch (e: Exception) {
        BigDecimal.ZERO
    }

    val isProfit = pnl >= BigDecimal.ZERO
    val pnlColor = if (isProfit) {
        if (quoteColorPref) {
            MixinAppTheme.colors.walletRed
        } else {
            MixinAppTheme.colors.walletGreen
        }
    } else {
        if (quoteColorPref) {
            MixinAppTheme.colors.walletGreen
        } else {
            MixinAppTheme.colors.walletRed
        }
    }
    
    val displaySymbol = position.displaySymbol ?: position.tokenSymbol ?: "Unknown"
    val quantity = try {
        val qty = BigDecimal(position.quantity)
        String.format("%.4f", qty)
    } catch (e: Exception) {
        position.quantity
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoilImage(
                model = position.iconUrl,
                placeholder = R.drawable.ic_avatar_place_holder,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sideText = if (position.side.lowercase() == "long") {
                        stringResource(R.string.Long)
                    } else {
                        stringResource(R.string.Short)
                    }
                    Text(
                        text = sideText,
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = displaySymbol,
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$quantity ${position.tokenSymbol ?: ""}",
                    fontSize = 12.sp,
                    color = MixinAppTheme.colors.textAssist
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = String.format("$%.2f", pnl.abs()),
                fontSize = 14.sp,
                color = pnlColor
            )
        }
    }
}
