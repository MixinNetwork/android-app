package one.mixin.android.ui.address.page

import PageScaffold
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.android.awaitFrame
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.response.Web3Token
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.address.component.TokenInfoHeader
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.vo.WithdrawalMemoPossibility
import one.mixin.android.vo.safe.TokenItem

@Composable
fun AddressInputPage(
    token: TokenItem?,
    web3Token: Web3Token?,
    contentText: String = "",
    onNext: (String) -> Unit,
    pop: () -> Unit,
    onScan: (() -> Unit)? = null,
) {
    var address by remember(contentText) { mutableStateOf(contentText) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect (Unit) {
        awaitFrame()
        focusRequester.requestFocus()
    }

    val context = LocalContext.current
    val memoEnabled = token?.withdrawalMemoPossibility == WithdrawalMemoPossibility.POSITIVE
    PageScaffold(
        title = stringResource(R.string.Address),
        subTitle = if (memoEnabled) "1/3" else "1/2",
        verticalScrollable = false,
        pop = pop,
        actions = {
            IconButton(onClick = {
                context.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_support),
                    contentDescription = null,
                    tint = MixinAppTheme.colors.icon,
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Column {
                TokenInfoHeader(token = token, web3Token = web3Token)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .cardBackground(
                            Color.Transparent,
                            MixinAppTheme.colors.borderColor,
                            cornerRadius = 8.dp
                        ),
                ) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.height(96.dp).focusRequester(focusRequester),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            backgroundColor = Color.Transparent,
                            textColor = MixinAppTheme.colors.textPrimary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            cursorColor = MixinAppTheme.colors.textBlue
                        ),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.hint_address),
                                color = MixinAppTheme.colors.textAssist,
                                fontSize = 14.sp
                            )
                        },
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = MixinAppTheme.colors.textPrimary,
                            textAlign = TextAlign.Start
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text, imeAction = ImeAction.Done
                        ),
                        minLines = 3,
                        maxLines = 3
                    )

                    if (address.isNotBlank()) {
                        IconButton(
                            onClick = {
                                address = ""
                            }, modifier = Modifier.align(Alignment.BottomEnd)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_addr_remove),
                                contentDescription = null,
                                tint = MixinAppTheme.colors.textPrimary
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                onScan?.invoke()
                            }, modifier = Modifier.align(Alignment.BottomEnd)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_addr_qr),
                                contentDescription = null,
                                tint = MixinAppTheme.colors.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    onClick = {
                        onNext.invoke(address)
                    },
                    enabled = address.isBlank().not(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = if (address.isNullOrBlank()
                                .not()
                        ) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGrayLight,
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
                        text = stringResource(R.string.Next),
                        color = if (address.isBlank()
                        ) MixinAppTheme.colors.textAssist else Color.White,
                    )
                }
            }
        }
    }
}