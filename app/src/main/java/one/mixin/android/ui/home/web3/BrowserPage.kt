package one.mixin.android.ui.home.web3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import one.mixin.android.R
import one.mixin.android.tip.wc.internal.TipGas
import one.mixin.android.tip.wc.internal.displayValue
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.ui.tip.wc.sessionrequest.FeeInfo
import one.mixin.android.vo.priceUSD
import one.mixin.android.vo.safe.Token
import java.math.BigDecimal

@Composable
fun BrowserPage(tipGas: TipGas?, asset: Token?, showPin: () -> Unit) {
    MixinAppTheme {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.height(20.dp))
            val fee = tipGas?.displayValue() ?: BigDecimal.ZERO
            if (fee == BigDecimal.ZERO) {
                FeeInfo(
                    amount = "$fee",
                    fee = fee.multiply(asset.priceUSD()),
                )
            } else {
                FeeInfo(
                    amount = "$fee ${asset?.symbol}",
                    fee = fee.multiply(asset.priceUSD()),
                )
            }
            Box(modifier = Modifier.height(20.dp))
            if (tipGas == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterHorizontally),
                    color = MixinAppTheme.colors.accent,
                )
            } else {
                Button(
                    onClick = showPin,
                    colors =
                    ButtonDefaults.outlinedButtonColors(
                        backgroundColor = MixinAppTheme.colors.accent,
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 36.dp, vertical = 11.dp),
                ) {
                    Text(text = stringResource(id = R.string.Continue), color = Color.White)
                }
            }
        }
    }
}