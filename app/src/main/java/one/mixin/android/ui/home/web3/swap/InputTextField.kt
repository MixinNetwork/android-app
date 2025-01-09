package one.mixin.android.ui.home.web3.swap

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import one.mixin.android.R
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.widget.CoilRoundedHexagonTransformation
import java.math.BigDecimal

@SuppressLint("UnrememberedMutableState")
@Composable
fun InputContent(
    token: SwapToken?,
    text: String,
    selectClick: () -> Unit,
    onInputChanged: ((String) -> Unit)? = null,
    readOnly: Boolean = false,
) {
    if (readOnly) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Left(token, selectClick)

                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = text,
                            style =
                                TextStyle(
                                    fontSize = 20.sp,
                                    color = MixinAppTheme.colors.textPrimary,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.End,
                                ),
                        )
                    }
                }
            }
            Text(text = "", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Light)) // placeholder
        }
    } else {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        val interactionSource = remember { MutableInteractionSource() }
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Left(token, selectClick)
                BasicTextField(
                    value = text,
                    onValueChange = {
                        val v =
                            try {
                                if (it.isBlank()) BigDecimal.ZERO else BigDecimal(it)
                            } catch (e: Exception) {
                                return@BasicTextField
                            }
                        onInputChanged?.invoke(it)
                    },
                    maxLines = 1,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    keyboardController?.show()
                                }
                            },
                    interactionSource = interactionSource,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    textStyle =
                        TextStyle(
                            fontSize = 20.sp,
                            color = MixinAppTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                        ),
                    cursorBrush = SolidColor(MixinAppTheme.colors.accent),
                ) { innerTextField ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.fillMaxWidth()) {
                            innerTextField()
                        }
                    }
                }
            }
            Box(modifier = Modifier.width(8.dp))
            Text(text = "", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Light)) // placeholder
        }
    }
}

@Composable
private fun Left(
    token: SwapToken?,
    selectClick: () -> Unit,
) {
    Row(modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { selectClick.invoke() }, verticalAlignment = Alignment.CenterVertically) {
        if (token?.collectionHash != null) {
            CoilImage(
                model = ImageRequest.Builder(LocalContext.current).data(token.icon).transformations(CoilRoundedHexagonTransformation()).build(),
                placeholder = R.drawable.ic_inscription_icon,
                modifier = Modifier
                    .size(32.dp),
            )
        } else {
            CoilImage(
                model = token?.icon ?: "",
                placeholder = R.drawable.ic_avatar_place_holder,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
            )
        }
        Box(modifier = Modifier.width(10.dp))
        Text(
            text = token?.symbol ?: "",
            style =
                TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MixinAppTheme.colors.textPrimary,
                ),
        )
        Box(modifier = Modifier.width(4.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_web3_drop_down),
            contentDescription = null,
            tint = MixinAppTheme.colors.icon,
        )
        Box(modifier = Modifier.width(10.dp))
    }
}
