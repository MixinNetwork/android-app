package one.mixin.android.ui.wallet.alert

import PageScaffold
import android.content.Context
import android.graphics.Rect
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.Card
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
import androidx.lifecycle.findViewTreeLifecycleOwner
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.wallet.alert.components.AlertFrequencySelector
import one.mixin.android.ui.wallet.alert.components.AlertTypeSelector
import one.mixin.android.ui.wallet.alert.components.PercentagesRow
import one.mixin.android.ui.wallet.alert.vo.Alert
import one.mixin.android.ui.wallet.alert.vo.AlertFrequency
import one.mixin.android.ui.wallet.alert.vo.AlertRequest
import one.mixin.android.ui.wallet.alert.vo.AlertType
import one.mixin.android.ui.wallet.alert.vo.InputError
import one.mixin.android.vo.safe.TokenItem
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
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
            title = stringResource(id = R.string.Edit_Alert),
            verticalScrollable = false,
            pop = pop,
        ) {
            if (token != null) {
                val context = LocalContext.current
                val currentPrice = BigDecimal(token.priceUsd)
                var alertPrice by remember { mutableStateOf(currentPrice.priceFormat()) }
                val maxPrice = currentPrice.multiply(BigDecimal(100))
                val minPrice = currentPrice.divide(BigDecimal(100))
                val focusManager = LocalFocusManager.current
                val keyboardController = LocalSoftwareKeyboardController.current
                var selectedAlertType by remember { mutableStateOf(alert?.type ?: AlertType.PRICE_REACHED) }
                var selectedAlertFrequency by remember { mutableStateOf(alert?.frequency ?: AlertFrequency.ONCE) }
                var isLoading by remember { mutableStateOf(false) }
                var inputError by remember { mutableStateOf<InputError?>(null) }
                val viewModel = hiltViewModel<AlertViewModel>()
                val coroutineScope = rememberCoroutineScope()

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
                        modifier = Modifier
                            .fillMaxWidth(), horizontalAlignment = Alignment.Start
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
                                Text(text = stringResource(R.string.Current_price, "${BigDecimal(token.priceUsd).priceFormat()} USD"), fontSize = 13.sp, color = MixinAppTheme.colors.textAssist)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        AlertTypeSelector(selectedType = selectedAlertType) { newType ->
                            if (selectedAlertType != newType) {
                                alertPrice = if (newType in listOf(AlertType.PRICE_REACHED, AlertType.PRICE_DECREASED, AlertType.PRICE_INCREASED)) {
                                    currentPrice.toPlainString()
                                } else {
                                    ""
                                }
                                selectedAlertType = newType
                            }
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
                                        "${stringResource(R.string.Price)} (USD)"
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
                                                alertPrice = newPrice.toPlainString().let {
                                                    if (newValue.endsWith(".")){
                                                        "$it."
                                                    }else{
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
                                                alertPrice = ""
                                                inputError = null
                                            }
                                        } else {
                                            if (selectedAlertType == AlertType.PERCENTAGE_INCREASED) {
                                                val newPercentage = newValue.toBigDecimalOrNull()
                                                if (newPercentage != null) {
                                                    val adjustedPercentage = newPercentage.coerceIn(BigDecimal("0.01"), BigDecimal("1000"))
                                                    alertPrice = adjustedPercentage.setScale(2, RoundingMode.DOWN).toPlainString()
                                                    inputError = null
                                                } else {
                                                    alertPrice = ""
                                                    inputError = null
                                                }
                                            } else if (selectedAlertType == AlertType.PERCENTAGE_DECREASED) {
                                                val newPercentage = newValue.toBigDecimalOrNull()
                                                if (newPercentage != null) {
                                                    val adjustedPercentage = newPercentage.coerceIn(BigDecimal("0.01"), BigDecimal("99"))
                                                    alertPrice = adjustedPercentage.setScale(2, RoundingMode.DOWN).toPlainString()
                                                    inputError = null
                                                } else {
                                                    alertPrice = ""
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
                                        color = MixinAppTheme.colors.textPrimary,
                                        textAlign = TextAlign.Start,
                                    ),
                                )
                            }
                        }

                        if (inputError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when (inputError) {
                                    InputError.EQUALS_CURRENT_PRICE -> "The set price cannot be the current price."
                                    InputError.EXCEEDS_MAX_PRICE -> "The price exceeds the maximum allowed value."
                                    InputError.BELOW_MIN_PRICE -> "The price is below the minimum allowed value."
                                    InputError.MUST_BE_LESS_THAN_CURRENT_PRICE -> "The price must be less than the current price."
                                    InputError.MUST_BE_GREATER_THAN_CURRENT_PRICE -> "The price must be greater than the current price."
                                    else -> ""
                                },
                                color = Color(0xffDB454F),
                                fontSize = 12.sp,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        PercentagesRow(modifier = Modifier.fillMaxWidth(), selectedAlertType) { percentage ->
                            if (selectedAlertType in listOf(AlertType.PRICE_REACHED, AlertType.PRICE_DECREASED, AlertType.PRICE_INCREASED)) {
                                val newPrice = currentPrice.multiply(BigDecimal.ONE.add(percentage.toBigDecimal()))
                                alertPrice = newPrice.priceFormat()
                                inputError = null
                            } else {
                                alertPrice = when (percentage) {
                                    0.2f -> "20"
                                    0.1f -> "10"
                                    0.05f -> "5"
                                    -0.05f -> "5"
                                    -0.1f -> "10"
                                    else -> "20"
                                }
                                inputError = null
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        AlertFrequencySelector(selectedAlertFrequency) { newFrequency ->
                            selectedAlertFrequency = newFrequency
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isLoading && !(inputError != null || alertPrice.isBlank()),
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            val alertRequest = AlertRequest(
                                assetId = token.assetId,
                                type = selectedAlertType.value,
                                value = alertPrice.replace(",", ""),
                                frequency = selectedAlertFrequency.value,
                                lang = Locale.getDefault().language,
                            )

                            coroutineScope.launch {
                                isLoading = true
                                val re = viewModel.add(alertRequest)
                                isLoading = false
                                Timber.e("${re?.data}")
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = if (inputError != null || alertPrice.isBlank()) MixinAppTheme.colors.backgroundGrayLight else MixinAppTheme.colors.accent,
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
                                color = if (inputError != null || alertPrice.isBlank()) MixinAppTheme.colors.textAssist else Color.White,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
