package one.mixin.android.ui.wallet.alert

import PageScaffold
import android.content.Context
import android.graphics.Rect
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.removeEnd
import one.mixin.android.ui.wallet.alert.components.AlertFrequencySelector
import one.mixin.android.ui.wallet.alert.components.AlertTypeSelector
import one.mixin.android.ui.wallet.alert.components.PercentagesRow
import one.mixin.android.ui.wallet.alert.vo.Alert
import one.mixin.android.ui.wallet.alert.vo.AlertFrequency
import one.mixin.android.ui.wallet.alert.vo.AlertRequest
import one.mixin.android.ui.wallet.alert.vo.AlertType
import one.mixin.android.ui.wallet.alert.vo.AlertUpdateRequest
import one.mixin.android.ui.wallet.alert.vo.InputError
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

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
            title = stringResource(id = if (alert == null) R.string.Add_Alert else R.string.Edit_Alert),
            verticalScrollable = false,
            pop = pop,
        ) {
            if (token != null) {
                val context = LocalContext.current
                val currentPrice = BigDecimal(token.priceUsd)
                var alertValue by remember { mutableStateOf(alert?.rawValue ?: currentPrice.toPlainString()) }
                val maxPrice = currentPrice.multiply(BigDecimal(100))
                val minPrice = currentPrice.divide(BigDecimal(100))
                val focusManager = LocalFocusManager.current
                val keyboardController = LocalSoftwareKeyboardController.current
                var selectedAlertType by remember { mutableStateOf(alert?.type ?: AlertType.PRICE_REACHED) }
                var selectedAlertFrequency by remember { mutableStateOf(alert?.frequency ?: AlertFrequency.ONCE) }
                var isLoading by remember { mutableStateOf(false) }
                var inputError by remember { mutableStateOf(if (alertValue.toBigDecimalOrNull() == currentPrice) InputError.EQUALS_CURRENT_PRICE else null) }
                val viewModel = hiltViewModel<AlertViewModel>()
                val coroutineScope = rememberCoroutineScope()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp)
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(), horizontalAlignment = Alignment.Start
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp)) {
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
                                Text(text = stringResource(R.string.Current_price, "${BigDecimal(token.priceUsd).priceFormat()} USD"), fontSize = 13.sp, color = MixinAppTheme.colors.textAssist)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        AlertTypeSelector(selectedType = selectedAlertType) { newType ->
                            if (selectedAlertType != newType) {
                                alertValue = if (newType in listOf(AlertType.PRICE_REACHED, AlertType.PRICE_DECREASED, AlertType.PRICE_INCREASED)) {
                                    inputError = InputError.EQUALS_CURRENT_PRICE
                                    currentPrice.toPlainString()
                                } else {
                                    inputError = null
                                    ""
                                }
                                selectedAlertType = newType
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .draw9Patch(context, MixinAppTheme.drawables.bgAlertCard),
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 23.dp)
                                    .padding(top = 19.dp, bottom = 22.dp)
                            ) {
                                Text(
                                    text = if (selectedAlertType in listOf(AlertType.PRICE_REACHED, AlertType.PRICE_DECREASED, AlertType.PRICE_INCREASED)) {
                                        stringResource(R.string.price_in_currency, "USD")
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
                                    value = alertValue,
                                    onValueChange = { newValue ->
                                        if (selectedAlertType in listOf(AlertType.PRICE_REACHED, AlertType.PRICE_DECREASED, AlertType.PRICE_INCREASED)) {
                                            val newPrice = newValue.replace(",", "").toBigDecimalOrNull()
                                            if (newPrice != null) {
                                                alertValue = newPrice.toPlainString().let {
                                                    if (newValue.endsWith(".")) {
                                                        "$it."
                                                    } else {
                                                        it
                                                    }
                                                }
                                                inputError = if (newPrice == currentPrice) {
                                                    InputError.EQUALS_CURRENT_PRICE
                                                } else if (newPrice > maxPrice) {
                                                    InputError.EXCEEDS_MAX_PRICE
                                                } else if (newPrice < minPrice) {
                                                    InputError.BELOW_MIN_PRICE
                                                } else if (selectedAlertType == AlertType.PRICE_DECREASED && newPrice > currentPrice) {
                                                    InputError.MUST_BE_LESS_THAN_CURRENT_PRICE
                                                } else if (selectedAlertType == AlertType.PRICE_INCREASED && newPrice < currentPrice) {
                                                    InputError.MUST_BE_GREATER_THAN_CURRENT_PRICE
                                                } else {
                                                    null
                                                }
                                            } else {
                                                alertValue = ""
                                                inputError = null
                                            }
                                        } else {
                                            if (newValue.replace("%", "") == "0.0") {
                                                alertValue = "0.0"
                                                inputError = null
                                                return@BasicTextField
                                            }
                                            var dot: Boolean
                                            val newPercentage = newValue.replace("%", "").let {
                                                if (it.endsWith(".")) {
                                                    dot = true
                                                    it.removeEnd(".")
                                                } else {
                                                    dot = false
                                                    it
                                                }
                                            }.toBigDecimalOrNull()
                                            if (selectedAlertType == AlertType.PERCENTAGE_INCREASED) {
                                                if (newPercentage != null) {
                                                    val adjustedPercentage = newPercentage.setScale(2, RoundingMode.DOWN)
                                                    alertValue = adjustedPercentage.stripTrailingZeros().toPlainString().let {
                                                        if (dot) {
                                                            "$it.%"
                                                        } else {
                                                            "$it%"
                                                        }
                                                    }
                                                    inputError = if (adjustedPercentage.toFloat() > 1000f) {
                                                        InputError.INCREASE_TOO_HIGH
                                                    } else if (adjustedPercentage.toFloat() < 0.01f) {
                                                        InputError.INCREASE_TOO_LOW
                                                    } else {
                                                        null
                                                    }
                                                } else {
                                                    alertValue = ""
                                                    inputError = null
                                                }
                                            } else if (selectedAlertType == AlertType.PERCENTAGE_DECREASED) {
                                                if (newPercentage != null) {
                                                    val adjustedPercentage = newPercentage.setScale(2, RoundingMode.DOWN)
                                                    alertValue = adjustedPercentage.stripTrailingZeros().toPlainString().let {
                                                        if (dot) {
                                                            "$it.%"
                                                        } else {
                                                            "$it%"
                                                        }
                                                    }
                                                    inputError = if (adjustedPercentage.toFloat() > 99.99f) {
                                                        InputError.DECREASE_TOO_HIGH
                                                    } else if (adjustedPercentage.toFloat() < 0.01f) {
                                                        InputError.DECREASE_TOO_LOW
                                                    } else {
                                                        null
                                                    }
                                                } else {
                                                    alertValue = ""
                                                    inputError = null
                                                }
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
                                        lineHeight = 16.sp,
                                        color = if (inputError != null) Color(0xFFDB454F) else MixinAppTheme.colors.textPrimary,
                                        textAlign = TextAlign.Start,
                                    ),
                                    decorationBox = { innerTextField ->
                                        if (alertValue.isEmpty()) {
                                            Text(
                                                if (selectedAlertType in listOf(AlertType.PRICE_REACHED, AlertType.PRICE_DECREASED, AlertType.PRICE_INCREASED)) {
                                                    "0.00"
                                                } else {
                                                    "0.00%"
                                                },
                                                fontSize = 16.sp,
                                                lineHeight = 16.sp,
                                                color = MixinAppTheme.colors.textAssist,
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }

                        if (inputError != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Column(modifier = Modifier.padding(horizontal = 10.dp)) {
                                Text(
                                    text = when (inputError) {
                                        InputError.EQUALS_CURRENT_PRICE -> stringResource(R.string.error_equals_current_price)
                                        InputError.EXCEEDS_MAX_PRICE -> stringResource(R.string.error_exceeds_max_price)
                                        InputError.BELOW_MIN_PRICE -> stringResource(R.string.error_below_min_price)
                                        InputError.MUST_BE_LESS_THAN_CURRENT_PRICE -> stringResource(R.string.error_must_be_less_than_current_price)
                                        InputError.MUST_BE_GREATER_THAN_CURRENT_PRICE -> stringResource(R.string.error_must_be_greater_than_current_price)
                                        InputError.INCREASE_TOO_HIGH -> stringResource(R.string.error_increase_too_high)
                                        InputError.INCREASE_TOO_LOW -> stringResource(R.string.error_increase_too_low)
                                        InputError.DECREASE_TOO_HIGH -> stringResource(R.string.error_decrease_too_high)
                                        InputError.DECREASE_TOO_LOW -> stringResource(R.string.error_decrease_too_low)
                                        else -> ""
                                    },
                                    color = Color(0xFFDB454F),
                                    fontSize = 12.sp,
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        PercentagesRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp), selectedAlertType
                        ) { percentage ->
                            if (selectedAlertType in listOf(AlertType.PRICE_REACHED, AlertType.PRICE_DECREASED, AlertType.PRICE_INCREASED)) {
                                val newPrice = currentPrice.multiply(BigDecimal.ONE.add(percentage.toBigDecimal()))
                                alertValue = newPrice.toPlainString()
                                inputError = null
                            } else {
                                alertValue = when (percentage) {
                                    0.2f -> "20%"
                                    0.1f -> "10%"
                                    0.05f -> "5%"
                                    -0.05f -> "5%"
                                    -0.1f -> "10%"
                                    else -> "20%"
                                }
                                inputError = null
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        AlertFrequencySelector(selectedAlertFrequency) { newFrequency ->
                            selectedAlertFrequency = newFrequency
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    val enable = inputError == null &&
                        alertValue.isNotBlank() &&
                        (alertValue.replace("%", "").toBigDecimalOrNull() ?: BigDecimal.ZERO).compareTo(BigDecimal.ZERO) != 0
                    Button(
                        modifier = Modifier
                            .height(48.dp)
                            .align(alignment = Alignment.CenterHorizontally),
                        enabled = !isLoading && enable,
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()

                            coroutineScope.launch {
                                isLoading = true
                                if (alert != null) {
                                    val alertRequest = AlertUpdateRequest(
                                        type = selectedAlertType.value,
                                        value = if (selectedAlertType in listOf(AlertType.PERCENTAGE_DECREASED, AlertType.PERCENTAGE_INCREASED)) {
                                            alertValue.let {
                                                (it.replace("%", "").toFloat() / 100f).toString()
                                            }
                                        } else {
                                            alertValue.replace(",", "")
                                        },
                                        frequency = selectedAlertFrequency.value,
                                    )
                                    val re = viewModel.updateAlert(alert.alertId, alertRequest)
                                    if (re?.isSuccess == true) {
                                        pop.invoke()
                                    }
                                } else {
                                    val alertRequest = AlertRequest(
                                        assetId = token.assetId,
                                        type = selectedAlertType.value,
                                        value = if (selectedAlertType in listOf(AlertType.PERCENTAGE_DECREASED, AlertType.PERCENTAGE_INCREASED)) {
                                            alertValue.let {
                                                (it.replace("%", "").toFloat() / 100f).toString()
                                            }
                                        } else {
                                            alertValue.replace(",", "")
                                        },
                                        frequency = selectedAlertFrequency.value,
                                        lang = Locale.getDefault().language,
                                    )
                                    val re = viewModel.add(alertRequest)
                                    if (re?.isSuccess == true) {
                                        pop.invoke()
                                    }
                                }
                                isLoading = false

                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = if (enable) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGrayLight,
                        ),
                        shape = RoundedCornerShape(32.dp),
                        elevation = ButtonDefaults.elevation(
                            pressedElevation = 0.dp,
                            defaultElevation = 0.dp,
                            hoveredElevation = 0.dp,
                            focusedElevation = 0.dp,
                        ),
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                )
                            }
                            Text(
                                modifier = Modifier.alpha(if (isLoading) 0f else 1f),
                                text = stringResource(if (alert == null) R.string.Add_Alert else R.string.Save),
                                color = if (enable) Color.White else MixinAppTheme.colors.textAssist,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
