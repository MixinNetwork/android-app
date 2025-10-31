package one.mixin.android.ui.home.web3.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.home.web3.trade.ExpiryOption
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun PriceDisplay(
    fromToken: SwapToken?,
    toToken: SwapToken?,
    limitPrice: String,
    expiryOption: ExpiryOption,
    onExpiryChange: (ExpiryOption) -> Unit,
) {
    var isPriceInverted by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val price = limitPrice.toBigDecimalOrNull()

    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val priceText = if (price != null && price > BigDecimal.ZERO) {
                if (!isPriceInverted) {
                    "1 ${fromToken?.symbol} ≈ ${price.stripTrailingZeros().toPlainString()} ${toToken?.symbol}"
                } else {
                    val invertedPrice = BigDecimal.ONE.divide(price, 8, RoundingMode.HALF_UP)
                    "1 ${toToken?.symbol} ≈ ${invertedPrice.stripTrailingZeros().toPlainString()} ${fromToken?.symbol}"
                }
            } else {
                "..."
            }
            Text(
                text = priceText, color = MixinAppTheme.colors.textAssist, fontSize = 16.sp
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_price_switch), contentDescription = "Switch price", tint = MixinAppTheme.colors.iconGray, modifier = Modifier.clickable { isPriceInverted = !isPriceInverted })
        }
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.expiry), color = MixinAppTheme.colors.textAssist, fontSize = 14.sp
            )
            Box(modifier = Modifier.wrapContentSize()) {
                Row(
                    modifier = Modifier.clickable { expanded = true }, verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(expiryOption.labelRes), color = MixinAppTheme.colors.textAssist, fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_type_down), contentDescription = "Select expiry", tint = MixinAppTheme.colors.iconGray
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    ExpiryOption.entries.forEach { option ->
                        DropdownMenuItem(
                            onClick = {
                                expanded = false
                                onExpiryChange(option)
                            }) {
                            Text(text = stringResource(option.labelRes))
                        }
                    }
                }
            }
        }
    }
}