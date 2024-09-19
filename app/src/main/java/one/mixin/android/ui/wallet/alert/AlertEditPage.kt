package one.mixin.android.ui.wallet.alert

import PageScaffold
import android.content.Context
import android.graphics.Rect
import androidx.annotation.DrawableRes
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import one.mixin.android.R
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.ui.wallet.alert.vo.Alert
import one.mixin.android.ui.wallet.alert.vo.AlertFrequency
import one.mixin.android.ui.wallet.alert.vo.AlertRequest
import one.mixin.android.ui.wallet.alert.vo.AlertType
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.safe.TokenItem
import timber.log.Timber
import java.math.BigDecimal

fun Modifier.draw9Patch(
    context: Context,
    @DrawableRes ninePatchRes: Int,
) = this.drawBehind {
    drawIntoCanvas {
        ContextCompat.getDrawable(context, ninePatchRes)?.let { ninePatch ->
            ninePatch.run {
                bounds = Rect(0, 0, size.width.toInt(), size.height.toInt())
                draw(it.nativeCanvas)
            }
        }
    }

}

@Composable
fun AlertEditPage(token: TokenItem?, alert: Alert?, pop: () -> Unit) {
    MixinAppTheme {
        PageScaffold(
            title = stringResource(id = R.string.Edit_Alert),
            verticalScrollable = true,
            pop = pop,
        ) {
            if (token != null) {
                val context = LocalContext.current
                val currentPrice = token.priceFiat()
                var alertPrice by remember { mutableStateOf(token.priceFiat().toPlainString()) }
                val maxPrice = token.priceFiat().multiply(BigDecimal(100))
                val minPrice = token.priceFiat().divide(BigDecimal(100))
                val focusManager = LocalFocusManager.current
                val keyboardController = LocalSoftwareKeyboardController.current
                var selectedAlertType by remember { mutableStateOf(alert?.type ?: AlertType.PRICE_REACHED) }
                var selectedAlertFrequency by remember { mutableStateOf(alert?.frequency ?: AlertFrequency.ONCE) }
                var isLoading by remember { mutableStateOf(false) }
                var checkPrice by remember { mutableStateOf(false) }
                val viewModel = hiltViewModel<AlertViewModel>()
                val coroutineScope = rememberCoroutineScope()
                coroutineScope.launch {
                    viewModel.alerts()
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start
                    ) {
                        Row {
                            CoilImage(
                                model = token.iconUrl,
                                placeholder = R.drawable.ic_avatar_place_holder,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = token.symbol, fontSize = 16.sp, color = MixinAppTheme.colors.textPrimary)
                                Text(text = stringResource(R.string.Current_price, "${token.priceFiat().priceFormat()} ${Fiats.getAccountCurrencyAppearance()}"), fontSize = 13.sp, color = MixinAppTheme.colors.textAssist)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        AlertTypeSelector(selectedType = selectedAlertType) { newType ->
                            selectedAlertType = newType
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(), backgroundColor = MixinAppTheme.colors.background
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                            ) {
                                Text(
                                    text = if (selectedAlertType in listOf(AlertType.PRICE_REACHED, AlertType.PRICE_DECREASED, AlertType.PRICE_INCREASED)) {
                                        "${stringResource(R.string.Price)} (${Fiats.getAccountCurrencyAppearance()})"
                                    } else {
                                        stringResource(R.string.Value)
                                    }, fontSize = 12.sp, color = MixinAppTheme.colors.textAssist
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                val focusRequester = remember { FocusRequester() }
                                val interactionSource = remember { MutableInteractionSource() }
                                val focused = remember { mutableStateOf(false) }

                                Spacer(modifier = Modifier.height(10.dp))
                                BasicTextField(
                                    value = alertPrice,
                                    onValueChange = { newValue ->
                                        if (selectedAlertType in listOf(AlertType.PRICE_REACHED, AlertType.PRICE_DECREASED, AlertType.PRICE_INCREASED)) {
                                            val newPrice = newValue.toBigDecimalOrNull()
                                            if (newPrice != null) {
                                                checkPrice = !(newPrice < minPrice || newPrice > maxPrice || newPrice == currentPrice)
                                                alertPrice = newPrice.toPlainString()
                                            } else {
                                                alertPrice = ""
                                                checkPrice = false
                                            }
                                        } else {
                                            val value = newValue.toFloatOrNull()
                                            if (value != null) {
                                                checkPrice = !(value < 0.01f || value > 1000)
                                                alertPrice = value.toString()
                                            } else {
                                                alertPrice = ""
                                                checkPrice = false
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .onFocusChanged {
                                            if (it.isFocused) {
                                                keyboardController?.show()
                                            }
                                            focused.value = it.isFocused
                                        },
                                    interactionSource = interactionSource,
                                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                    textStyle = TextStyle(
                                        fontSize = 16.sp,
                                        color = MixinAppTheme.colors.textPrimary,
                                        textAlign = TextAlign.Start,
                                    ),
                                )
                            }
                        }

                        if (alertPrice.toBigDecimalOrNull() == currentPrice) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "The set price cannot be the current price.",
                                color = Color(0xffDB454F),
                                fontSize = 12.sp,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        PercentagesRow(modifier = Modifier.fillMaxWidth()) { percentage ->
                            if (selectedAlertType in listOf(AlertType.PRICE_REACHED, AlertType.PRICE_DECREASED, AlertType.PRICE_INCREASED)) {
                                val newPrice = currentPrice.multiply(BigDecimal.ONE.add(percentage.toBigDecimal()))
                                alertPrice = newPrice.toPlainString()
                                checkPrice = true
                            } else {
                                alertPrice = when (percentage) {
                                    0.2f -> "20"
                                    0.1f -> "10"
                                    0.05f -> "5"
                                    -0.05f -> "5"
                                    -0.1f -> "10"
                                    else -> "-20"
                                }
                                checkPrice = true
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        AlertFrequencySelector(selectedAlertFrequency) { newFrequency ->
                            selectedAlertFrequency = newFrequency
                        }
                    }

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isLoading && checkPrice,
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            val alertRequest = AlertRequest(
                                assetId = token.assetId,
                                type = selectedAlertType.value,
                                value = alertPrice,
                                frequency = selectedAlertFrequency.value,
                                lang = "zh",
                            )

                            coroutineScope.launch {
                                isLoading = true
                                val re = viewModel.add(alertRequest)
                                isLoading = false
                                Timber.e("${re?.data}")
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = if (!checkPrice) MixinAppTheme.colors.backgroundGrayLight else MixinAppTheme.colors.accent,
                        ),
                        shape = RoundedCornerShape(32.dp),
                        elevation = ButtonDefaults.elevation(
                            pressedElevation = 0.dp,
                            defaultElevation = 0.dp,
                            hoveredElevation = 0.dp,
                            focusedElevation = 0.dp,
                        ),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.Add_Alert),
                                color = if (!checkPrice) MixinAppTheme.colors.textAssist else Color.White,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
