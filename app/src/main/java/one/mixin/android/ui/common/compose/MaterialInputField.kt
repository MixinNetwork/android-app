package one.mixin.android.ui.common.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun MaterialInputField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    enabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (isFocused || value.isNotEmpty()) 1f else 0f,
        animationSpec = tween(200),
        label = "label_animation"
    )
    
    val labelColor = MixinAppTheme.colors.accent
    
    val backgroundColor = MixinAppTheme.colors.backgroundWindow
    
    val borderColor = Color.Transparent

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = if (isFocused) 2.dp else 0.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .padding(top = 14.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                    },
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = singleLine,
                enabled = enabled,
                cursorBrush = SolidColor(labelColor),
                textStyle = MaterialTheme.typography.body1.copy(color = MixinAppTheme.colors.textPrimary),
                decorationBox = { innerTextField ->
                    Box {
                        Column {
                            AnimatedVisibility(
                                visible = animatedProgress > 0f,
                                enter = fadeIn(animationSpec = tween(200)) +
                                        slideInVertically(animationSpec = tween(200)) { -it },
                                exit = fadeOut(animationSpec = tween(200)) +
                                        slideOutVertically(animationSpec = tween(200)) { -it }
                            ) {
                                Text(
                                    text = hint,
                                    style = MaterialTheme.typography.subtitle1,
                                    color = labelColor,
                                    modifier = Modifier.offset(y = (-20).dp)
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier.offset(y = if (animatedProgress > 0f) 8.dp else -10.dp)
                        ) {
                            if (value.isEmpty() && !isFocused) {
                                Text(
                                    text = hint,
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )
        }
    }
}
