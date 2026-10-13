package one.mixin.android.ui.home.web3.trade.perps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.compose.theme.MixinAppTheme
import java.math.BigDecimal

@Composable
internal fun PerpsCloseMargin(pnl: BigDecimal?, margin: BigDecimal?) {
    Column {
        Text(stringResource(R.string.Margin).uppercase(), color = MixinAppTheme.colors.textRemarks, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Text(margin?.let { if (it.signum() < 0) formatPerpsSignedRawUsdDecimal(it) else formatPerpsRawUsdDecimal(it) } ?: "--", color = MixinAppTheme.colors.textPrimary, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        PerpsIncludedPnl(pnl)
    }
}

@Composable
internal fun PerpsIncludedPnl(pnl: BigDecimal?) {
    val quoteColorReversed = LocalContext.current.defaultSharedPreferences.getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    Row {
        Text(stringResource(R.string.perps_include_pnl), color = MixinAppTheme.colors.textAssist, fontSize = 12.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            text = pnl?.let(::formatPerpsSignedRawUsdDecimal) ?: "--",
            color = when {
                pnl == null || pnl.signum() == 0 -> MixinAppTheme.colors.textAssist
                pnl.signum() > 0 -> risingColor
                else -> fallingColor
            },
            fontSize = 12.sp,
        )
    }
}
