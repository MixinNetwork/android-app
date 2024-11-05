package one.mixin.android.ui.setting.ui.page

import PageScaffold
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
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.pxToDp
import one.mixin.android.extension.tickVibrate
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.ui.wallet.alert.AlertViewModel
import org.bouncycastle.math.raw.Mod

@Composable
fun MnemonicPhraseBackupPinPage(pop: () -> Unit, next: (String) -> Unit) {
    val context = LocalContext.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    var isLoading by remember { mutableStateOf(false) }
    var pinCode by remember { mutableStateOf("") }
    var errorInfo by remember { mutableStateOf("") }
    val viewModel = hiltViewModel<WalletViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val list = listOf(
        "1",
        "2",
        "3",
        "4",
        "5",
        "6",
        "7",
        "8",
        "9",
        "",
        "0",
        "<<",
    )
    MixinAppTheme {
        PageScaffold(
            title = stringResource(R.string.Mnemonic_Phrase),
            verticalScrollable = false,
            pop = pop,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(70.dp))
                Text(
                    stringResource(R.string.Enter_your_PIN),
                    color = MixinAppTheme.colors.textPrimary,
                    fontWeight = FontWeight.W600,
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    lineHeight = 27.sp
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
                                    if (index < pinCode.length) MixinAppTheme.colors.textPrimary
                                    else Color.Transparent
                                )
                                .border(1.dp, MixinAppTheme.colors.textPrimary, CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorInfo, modifier = Modifier.alpha(if (errorInfo.isNotBlank()) 1f else 0f), color = MixinAppTheme.colors.tipError)
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    onClick = {
                        isLoading = true
                        coroutineScope.launch {
                            runCatching {
                                viewModel.verifyPin(pinCode)
                            }.onSuccess { response ->
                                if (response.isSuccess) {
                                    next(pinCode)
                                } else {
                                    isLoading = false
                                    errorInfo = response.errorDescription
                                    pinCode = ""
                                }
                            }.onFailure { t ->
                                isLoading = false
                                errorInfo = t.message ?: ""
                                pinCode = ""
                            }
                        }
                    },
                    colors =
                    ButtonDefaults.outlinedButtonColors(
                        backgroundColor = if (pinCode.length < 6) MixinAppTheme.colors.backgroundGray else MixinAppTheme.colors.accent
                    ),
                    enabled = pinCode.length >= 6,
                    shape = RoundedCornerShape(32.dp),
                    elevation =
                    ButtonDefaults.elevation(
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
                            text = stringResource(R.string.Continue),
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(36.dp))

                Box(
                    modifier =
                    Modifier
                        .wrapContentHeight()
                        .heightIn(120.dp, 240.dp)
                        .onSizeChanged {
                            size = it
                        },
                ) {
                    LazyVerticalGrid(
                        modifier =
                        Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        columns = GridCells.Fixed(3),
                        content = {
                            items(list.size) { index ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier =
                                    Modifier
                                        .height(
                                            context.pxToDp(
                                                (
                                                    size.toSize().height -
                                                        context.dpToPx(
                                                            40f,
                                                        )
                                                    ) / 4,
                                            ).dp,
                                        )
                                        .clip(shape = RoundedCornerShape(8.dp))
                                        .background(
                                            when (index) {
                                                11 -> MixinAppTheme.colors.backgroundDark
                                                9 -> Color.Transparent
                                                else -> MixinAppTheme.colors.background
                                            },
                                        )
                                        .run {
                                            clickable {
                                                context.tickVibrate()
                                                if (index == 11) {
                                                    if (pinCode.isNotEmpty()) {
                                                        pinCode =
                                                            pinCode.substring(
                                                                0,
                                                                pinCode.length - 1,
                                                            )
                                                    }
                                                } else if (pinCode.length < 6) {
                                                    pinCode += list[index]
                                                    if (pinCode.length == 6) {
                                                        // Todo
                                                    }
                                                }
                                            }
                                        },
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
                        },
                    )
                }
            }
        }
    }
}