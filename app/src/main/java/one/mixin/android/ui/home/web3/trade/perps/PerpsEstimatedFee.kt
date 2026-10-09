package one.mixin.android.ui.home.web3.trade.perps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import java.math.BigDecimal

@Composable
internal fun PerpsEstimatedFee(fee: BigDecimal?, onTipClick: () -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.Estimated_Fee).uppercase(),
                color = MixinAppTheme.colors.textRemarks,
                fontSize = 14.sp,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                painter = painterResource(R.drawable.ic_tip),
                contentDescription = stringResource(R.string.Estimated_Fee),
                tint = MixinAppTheme.colors.textAssist,
                modifier = Modifier.size(16.dp).clickable(role = Role.Button, onClick = onTipClick),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = fee?.let(::formatPerpsRawUsdDecimal) ?: "--",
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 18.sp,
        )
    }
}
