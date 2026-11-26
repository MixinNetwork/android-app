package one.mixin.android.ui.address.page

import one.mixin.android.ui.home.web3.components.PageScaffold
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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import androidx.core.text.isDigitsOnly
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.ChainId.RIPPLE_CHAIN_ID
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.openUrl
import one.mixin.android.ui.address.component.TokenInfoHeader
import one.mixin.android.ui.wallet.alert.components.cardBackground
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.vo.safe.TokenItem

@Composable
fun MemoInputPage(
    token: TokenItem?,
    web3Token: Web3TokenItem?,
    address: String,
    networkName: String? = null,
    networkIconUrl: String? = null,
    contentText: String = "",
    errorInfo: String? = null,
    isLoading: Boolean = false,
    onNext: (String?) -> Unit,
    pop: () -> Unit,
    onScan: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val memoState = rememberTextFieldState(initialText = contentText)
    LaunchedEffect(contentText) {
        if (memoState.text.toString() != contentText) {
            memoState.setTextAndPlaceCursorAtEnd(contentText)
        }
    }
    val memo = memoState.text.toString()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        awaitFrame()
        focusRequester.requestFocus()
    }

    val isRippleChainId = token?.assetId == RIPPLE_CHAIN_ID
    val isValidMemo = isRippleChainId.not() || ((memo.isDigitsOnly() && (memo.toIntOrNull() ?: 0) != 0))

    PageScaffold(
        title = stringResource(if (isRippleChainId) R.string.Tag else R.string.Memo),
        verticalScrollable = false,
        pop = pop,
        actions = {
            IconButton(onClick = {
                context.openUrl(
                    Constants.HelpLink.CUSTOMER_SERVICE,
                    source = if (isRippleChainId) {
                        AnalyticsTracker.CustomerServiceSource.ADDRESS_BOOK_ADD_TAG
                    } else {
                        AnalyticsTracker.CustomerServiceSource.ADDRESS_BOOK_ADD_MEMO
                    },
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
                .padding(horizontal = 10.dp)
        ) {
            Column(modifier = Modifier.imePadding()) {
                if (networkName == null) {
                    TokenInfoHeader(token = token, web3Token = web3Token)
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
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
                    BasicTextField(
                        state = memoState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .focusRequester(focusRequester),
                        cursorBrush = SolidColor(MixinAppTheme.colors.textBlue),
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MixinAppTheme.colors.textPrimary,
                            textAlign = TextAlign.Start
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text, imeAction = ImeAction.Done
                        ),
                        lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3, maxHeightInLines = 3),
                        decorator = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                            ) {
                                if (memo.isEmpty()) {
                                    Text(
                                        text = stringResource(if (isRippleChainId) R.string.tag_placeholder else R.string.memo_placeholder),
                                        color = MixinAppTheme.colors.textAssist,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )

                    if (memo.isNotBlank()) {
                        IconButton(
                            onClick = {
                                memoState.clearText()
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
                                            memoState.setTextAndPlaceCursorAtEnd(it)
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
                if (networkName == null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(stringResource(R.string.Address), color = MixinAppTheme.colors.textAssist, fontSize = 14.sp, lineHeight = 17.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(address, color = MixinAppTheme.colors.textAssist, textAlign = TextAlign.End, fontSize = 14.sp, lineHeight = 17.sp)
                    }
                } else {
                    AddressInfoRows(
                        networkName = networkName,
                        networkIconUrl = networkIconUrl,
                        address = address,
                        memoTitle = R.string.Memo,
                        memo = null,
                    )
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
                        onNext.invoke(memo)
                    },
                    enabled = isValidMemo && !isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = if (isValidMemo) MixinAppTheme.colors.accent else MixinAppTheme.colors.backgroundGrayLight,
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
                            color = if (isValidMemo) Color.White else MixinAppTheme.colors.textAssist,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
