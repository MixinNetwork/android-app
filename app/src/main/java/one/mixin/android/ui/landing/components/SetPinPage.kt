package one.mixin.android.ui.landing.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.MixinTopAppBar
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.pxToDp
import one.mixin.android.extension.tickVibrate

@Composable
fun SetPinPage(
    next: (String) -> Unit,
    errorMessage: String = "",
    onRetry: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    var pinCode by remember { mutableStateOf("") }
    var confirmPinCode by remember { mutableStateOf("") }
    var isConfirmStep by remember { mutableStateOf(false) }
    var localErrorMessage by remember { mutableStateOf("") }
    
    val displayErrorMessage = if (localErrorMessage.isNotEmpty()) localErrorMessage else errorMessage
    
    val onPinSubmit: () -> Unit = {
        if (pinCode.length == 6) {
            if (!isConfirmStep) {
                isConfirmStep = true
                localErrorMessage = ""
            } else {
                if (pinCode == confirmPinCode) {
                    next(pinCode)
                } else {
                    localErrorMessage = context.getString(R.string.PIN_does_not_match)
                    pinCode = ""
                    confirmPinCode = ""
                    isConfirmStep = false
                }
            }
        }
    }
    
    val list = listOf(
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "<<"
    )
    
    MixinAppTheme {
        Column {
            MixinTopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = {
                        context.openUrl(Constants.HelpLink.TIP)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_support),
                            contentDescription = null,
                            tint = MixinAppTheme.colors.icon,
                        )
                    }
                },
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_set_up_pin),
                    tint = Color.Unspecified,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = if (!isConfirmStep) {
                        stringResource(R.string.Set_up_pin_desc_1)
                    } else {
                        stringResource(R.string.Set_up_pin_desc_2)
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W600,
                    color = MixinAppTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(70.dp))
                
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(204.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(6) { index ->
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index < (if (isConfirmStep) confirmPinCode.length else pinCode.length)) {
                                        MixinAppTheme.colors.textPrimary
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .border(1.dp, MixinAppTheme.colors.textPrimary, CircleShape)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = displayErrorMessage,
                    modifier = Modifier.alpha(if (displayErrorMessage.isNotBlank()) 1f else 0f),
                    color = MixinAppTheme.colors.red,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (displayErrorMessage.isNotEmpty() && onRetry != null) {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        onClick = {
                            onRetry()
                            pinCode = ""
                            confirmPinCode = ""
                            isConfirmStep = false
                            localErrorMessage = ""
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = MixinAppTheme.colors.accent
                        ),
                        shape = RoundedCornerShape(32.dp),
                        elevation = ButtonDefaults.elevation(
                            pressedElevation = 0.dp,
                            defaultElevation = 0.dp,
                            hoveredElevation = 0.dp,
                            focusedElevation = 0.dp,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.Retry),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(36.dp))
                }
                
                Box(
                    modifier = Modifier
                        .wrapContentHeight()
                        .heightIn(120.dp, 240.dp)
                        .onSizeChanged { size = it }
                ) {
                    LazyVerticalGrid(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        columns = GridCells.Fixed(3),
                        content = {
                            items(list.size) { index ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .height(
                                            context.pxToDp(
                                                (size.toSize().height - context.dpToPx(40f)) / 4
                                            ).dp
                                        )
                                        .clip(shape = RoundedCornerShape(8.dp))
                                        .background(
                                            when (index) {
                                                11 -> MixinAppTheme.colors.backgroundDark
                                                9 -> Color.Transparent
                                                else -> MixinAppTheme.colors.background
                                            }
                                        )
                                        .clickable {
                                            context.tickVibrate()
                                            if (index == 11) {
                                                if (isConfirmStep) {
                                                    if (confirmPinCode.isNotEmpty()) {
                                                        confirmPinCode = confirmPinCode.substring(0, confirmPinCode.length - 1)
                                                    }
                                                } else {
                                                    if (pinCode.isNotEmpty()) {
                                                        pinCode = pinCode.substring(0, pinCode.length - 1)
                                                    }
                                                }
                                            } else if (index != 9) {
                                                if (isConfirmStep) {
                                                    if (confirmPinCode.length < 6) {
                                                        confirmPinCode += list[index]
                                                        if (confirmPinCode.length == 6) {
                                                            onPinSubmit()
                                                        }
                                                    }
                                                } else {
                                                    if (pinCode.length < 6) {
                                                        pinCode += list[index]
                                                        if (pinCode.length == 6) {
                                                            onPinSubmit()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    if (index == 11) {
                                        Image(
                                            painter = painterResource(R.drawable.ic_delete),
                                            contentDescription = null,
                                        )
                                    } else if (index != 9) {
                                        Text(
                                            text = list[index],
                                            fontSize = 24.sp,
                                            color = MixinAppTheme.colors.textPrimary,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
