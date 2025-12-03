package one.mixin.android.ui.home.web3.trade

import android.annotation.SuppressLint
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.request.ImageRequest
import coil3.request.transformations
import one.mixin.android.R
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.home.inscription.component.AutoSizeText
import one.mixin.android.widget.CoilRoundedHexagonTransformation
import java.math.BigDecimal

@SuppressLint("UnrememberedMutableState")
@Composable
fun InputContent(
    token: SwapToken?,
    text: String,
    selectClick: (() -> Unit)?,
    onInputChanged: ((String) -> Unit)? = null,
    readOnly: Boolean = false,
    inlineEndCompose: (@Composable () -> Unit)? = null,
) {
    if (readOnly) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxWidth()) {
                        if (inlineEndCompose != null) {
                            inlineEndCompose.invoke()
                        } else {
                            AutoSizeText(
                                text = text,
                                color = if (text == "0") MixinAppTheme.colors.textRemarks else MixinAppTheme.colors.textPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                }
                Right(token, selectClick)
            }
            Text(text = "", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Light)) // placeholder
        }
    } else {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        val interactionSource = remember { MutableInteractionSource() }
        var textFieldValue by remember {
            mutableStateOf(
                TextFieldValue(
                    text = text,
                    selection = TextRange(text.length)
                )
            )
        }

        LaunchedEffect(text) {
            if (textFieldValue.text != text) {
                textFieldValue = TextFieldValue(
                    text = text,
                    selection = TextRange(text.length)
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Always render BasicTextField to keep focus
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it
                            val v = try {
                                if (it.text.isBlank()) BigDecimal.ZERO else BigDecimal(it.text)
                            } catch (e: Exception) {
                                return@BasicTextField
                            }
                            onInputChanged?.invoke(it.text)
                        },
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    keyboardController?.show()
                                }
                            }
                            .alpha(if (inlineEndCompose != null) 0f else 1f),
                        textStyle = TextStyle(
                            fontSize = when {
                                textFieldValue.text.length <= 15 -> 24.sp
                                else -> {
                                    val excess = textFieldValue.text.length - 15
                                    val reduction = excess * 2
                                    (24 - reduction).coerceAtLeast(16).sp
                                }
                            },
                            color = MixinAppTheme.colors.textPrimary,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Start,
                        ),
                        cursorBrush = SolidColor(MixinAppTheme.colors.textPrimary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        interactionSource = interactionSource,
                    )

                    if (text.isEmpty() && inlineEndCompose == null) {
                        AutoSizeText(
                            text = "0",
                            color = MixinAppTheme.colors.textRemarks,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                    }
                    
                    // Overlay inline compose (e.g., spinner) on top
                    if (inlineEndCompose != null) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            inlineEndCompose.invoke()
                        }
                    }
                }
                Right(token, selectClick)
            }
            Text(text = "", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Light)) // placeholder
        }
    }
}

@Composable
private fun Right(
    token: SwapToken?,
    selectClick: (() -> Unit)? = null,
) {
    Row(modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { selectClick?.invoke() }, verticalAlignment = Alignment.CenterVertically) {
        if (token?.collectionHash != null) {
            CoilImage(
                model = ImageRequest.Builder(LocalContext.current).data(token.icon).transformations(CoilRoundedHexagonTransformation()).build(),
                placeholder = R.drawable.ic_inscription_icon,
                modifier = Modifier
                    .size(30.dp),
            )
        } else {
            Box {
                CoilImage(
                    model = token?.icon ?: "",
                    placeholder = R.drawable.ic_avatar_place_holder,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                )

                CoilImage(
                    model = token?.chain?.icon ?: "",
                    placeholder = R.drawable.ic_avatar_place_holder,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(13.dp)
                        .border(1.dp, MixinAppTheme.colors.background, CircleShape)
                        .clip(CircleShape),
                )
            }
        }
        Box(modifier = Modifier.width(8.dp))
        BasicText(
            text = token?.symbol ?: "",
            modifier = Modifier.widthIn(max = 62.dp),
            maxLines = 2,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.W500,
                color = MixinAppTheme.colors.textPrimary,
            ),
            autoSize = TextAutoSize.StepBased(
                minFontSize = 6.sp,
                maxFontSize = 16.sp,
                stepSize = 1.sp
            )
        )
        if (selectClick != null) {
            Box(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_web3_drop_down),
                contentDescription = null,
                tint = MixinAppTheme.colors.iconGray,
            )
        }
    }
}
