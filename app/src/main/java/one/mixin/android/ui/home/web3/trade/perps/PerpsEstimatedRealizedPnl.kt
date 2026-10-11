package one.mixin.android.ui.home.web3.trade.perps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
internal fun PerpsEstimatedRealizedPnl(pnl: BigDecimal?, margin: BigDecimal?) {
    val quoteColorReversed = LocalContext.current.defaultSharedPreferences.getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
    val risingColor = if (quoteColorReversed) MixinAppTheme.colors.walletRed else MixinAppTheme.colors.walletGreen
    val fallingColor = if (quoteColorReversed) MixinAppTheme.colors.walletGreen else MixinAppTheme.colors.walletRed
    val percent = estimatedPerpsClosePnlPercent(pnl, margin)
    Column {
        Text(
            text = stringResource(R.string.perps_estimated_realized_pnl).uppercase(),
            color = MixinAppTheme.colors.textRemarks,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (pnl == null) "--" else {
                "${formatPerpsSignedUsdDecimal(pnl)} (${percent?.let { formatPerpsSignedPercent(it, withSign = false) } ?: "--"})"
            },
            color = when {
                pnl == null || pnl.signum() == 0 -> MixinAppTheme.colors.textPrimary
                pnl.signum() > 0 -> risingColor
                else -> fallingColor
            },
            fontSize = 18.sp,
        )
    }
}
