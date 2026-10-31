package one.mixin.android.ui.address.page

import one.mixin.android.ui.home.web3.components.PageScaffold
import androidx.compose.foundation.clickable
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
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
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
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.vo.safe.TokenItem

@Composable
fun AddressInputPage(
    token: TokenItem?,
    web3Token: Web3TokenItem?,
    networkChainId: String? = token?.chainId ?: web3Token?.chainId,
    networkName: String = token?.chainName ?: web3Token?.chainName.orEmpty(),
    onNetworkClick: (() -> Unit)? = null,
    contentText: String = "",
    onNext: (String) -> Unit,
    pop: () -> Unit,
    onScan: (() -> Unit)? = null,
    errorInfo: String? = null,
    isLoading: Boolean = false
) {
    var address by remember(contentText, networkChainId) { mutableStateOf(contentText) }
    val focusRequester = remember { FocusRequester() }
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        awaitFrame()
        focusRequester.requestFocus()
    }

    val context = LocalContext.current
    PageScaffold(
        title = stringResource(R.string.Add_address),
        verticalScrollable = false,
        pop = pop,
        actions = {
            IconButton(onClick = {
                context.openUrl(
                    Constants.HelpLink.CUSTOMER_SERVICE,
                    source = AnalyticsTracker.CustomerServiceSource.ADDRESS_BOOK_ADD_ADDRESS,
                    wallet = if (web3Token != null) {
                        AnalyticsTracker.TradeWallet.WEB3
                    } else {
                        AnalyticsTracker.TradeWallet.MAIN
                    },
                )
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
            Column(modifier = Modifier.imePadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .cardBackground(
                            Color.Transparent,
                            MixinAppTheme.colors.borderColor,
                            cornerRadius = 8.dp,
                        )
                        .then(if (onNetworkClick != null) Modifier.clickable(onClick = onNetworkClick) else Modifier)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.network),
                            color = MixinAppTheme.colors.textAssist,
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = networkName,
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 14.sp,
                        )
                    }
                    if (onNetworkClick != null) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sort_arrow_down),
                            contentDescription = null,
                            tint = MixinAppTheme.colors.iconGray,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
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
                        modifier = Modifier
                            .height(120.dp)
                            .focusRequester(focusRequester),
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
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        },
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MixinAppTheme.colors.textPrimary,
                            textAlign = TextAlign.Start
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
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
                                tint = MixinAppTheme.colors.iconGray
                            )
                        }
                    } else {
                        Row(modifier = Modifier.align(Alignment.BottomEnd)) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text?.toString()?.let {
                                            address = it
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_paste),
                                    contentDescription = null,
                                    tint = MixinAppTheme.colors.iconGray
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            IconButton(
                                onClick = {
                                    onScan?.invoke()
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_scan),
                                    contentDescription = null,
                                    tint = MixinAppTheme.colors.iconGray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = errorInfo ?: "",
                    color = MixinAppTheme.colors.red,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .align(Alignment.CenterHorizontally)
                        .alpha(if (errorInfo.isNullOrBlank()) 0f else 1f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    onClick = {
                        onNext.invoke(address)
                    },
                    enabled = address.isBlank().not() && !isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = if (address.isBlank().not()) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGrayLight,
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
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.Next),
                            fontSize = 16.sp,
                            color = if (address.isBlank()
                            ) MixinAppTheme.colors.textAssist else Color.White,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
