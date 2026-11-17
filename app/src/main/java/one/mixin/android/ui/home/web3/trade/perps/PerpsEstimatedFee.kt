package one.mixin.android.ui.home.web3.trade.perps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import java.math.BigDecimal

@Composable
internal fun PerpsEstimatedFee(
    fee: BigDecimal?,
    onTipClick: () -> Unit,
    title: String = stringResource(R.string.Estimated_Fee),
    amountFontSize: TextUnit = 18.sp,
    titleSpacing: Dp = 8.dp,
) {
    Column {
        PerpsInfoLabel(
            text = title.uppercase(),
            color = MixinAppTheme.colors.textRemarks,
            onTipClick = onTipClick,
        )
        Spacer(Modifier.height(titleSpacing))
        Text(
            text = fee?.let(::formatPerpsRawUsdDecimal) ?: "--",
            color = MixinAppTheme.colors.textPrimary,
            fontSize = amountFontSize,
        )
    }
}
