package one.mixin.android.ui.home.web3

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.web3.ParsedTx
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.toast
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.TipGas
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.ui.home.web3.components.ActionBottom
import one.mixin.android.ui.home.web3.components.MessagePreview
import one.mixin.android.ui.home.web3.components.SolanaParsedTxPreview
import one.mixin.android.ui.home.web3.components.TokenTransactionPreview
import one.mixin.android.ui.home.web3.components.TransactionPreview
import one.mixin.android.ui.home.web3.components.Warning
import one.mixin.android.ui.tip.wc.WalletConnectBottomSheetDialogFragment
import one.mixin.android.ui.tip.wc.compose.ItemContent
import one.mixin.android.ui.tip.wc.sessionrequest.FeeInfo
import one.mixin.android.vo.priceUSD
import one.mixin.android.vo.safe.Token
import one.mixin.android.web3.js.JsSignMessage
import one.mixin.android.web3.js.SolanaTxSource
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserPage(
    account: String,
    chain: Chain,
    amount: String?,
    token: Web3TokenItem?,
    toAddress: String?,
    type: Int,
    step: WalletConnectBottomSheetDialogFragment.Step,
    tipGas: TipGas?,
    solanaFee: BigDecimal?,
    parsedTx: ParsedTx?,
    solanaTxSource: SolanaTxSource,
    asset: Token?,
    transaction: WCEthereumTransaction?,
    data: String?,
    url: String?,
    title: String?,
    errorInfo: String?,
    insufficientGas: Boolean,
    showPin: () -> Unit,
    onPreviewMessage: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onRejectAction: () -> Unit,
) {
    MixinAppTheme {
        Column(
            modifier =
            Modifier
                .clip(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .fillMaxWidth()
                .fillMaxHeight()
                .background(MixinAppTheme.colors.background),
        ) {
            Column(
                modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(weight = 1f, fill = true),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.height(50.dp))
                when (step) {
                    WalletConnectBottomSheetDialogFragment.Step.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(70.dp),
                            color = MixinAppTheme.colors.accent,
                        )
                    }
                    WalletConnectBottomSheetDialogFragment.Step.Sending -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(70.dp),
                            color = MixinAppTheme.colors.accent,
                        )
                    }

                    WalletConnectBottomSheetDialogFragment.Step.Error -> {
                        Icon(
                            modifier = Modifier.size(70.dp),
                            painter = painterResource(id = R.drawable.ic_transfer_status_failed),
                            contentDescription = null,
                            tint = Color.Unspecified,
                        )
                    }

                    WalletConnectBottomSheetDialogFragment.Step.Done -> {
                        Icon(
                            modifier = Modifier.size(70.dp),
                            painter = painterResource(id = R.drawable.ic_transfer_status_success),
                            contentDescription = null,
                            tint = Color.Unspecified,
                        )
                    }

                    else -> {
                        Icon(
                            modifier = Modifier.size(70.dp),
                            painter =
                                painterResource(
                                    id = if (token != null) R.drawable.ic_web3_transaction else R.drawable.ic_no_dapp,
                                ),
                            contentDescription = null,
                            tint = Color.Unspecified,
                        )
                    }
                }

                Box(modifier = Modifier.height(16.dp))

                Text(
                    text =
                        stringResource(
                            id =
                                if (JsSignMessage.isSignMessage(type)) {
                                    when (step) {
                                        WalletConnectBottomSheetDialogFragment.Step.Loading -> R.string.web3_message_request
                                        WalletConnectBottomSheetDialogFragment.Step.Done -> R.string.web3_sending_success
                                        WalletConnectBottomSheetDialogFragment.Step.Error -> if (insufficientGas) R.string.insufficient_balance else R.string.web3_signing_failed
                                        WalletConnectBottomSheetDialogFragment.Step.Sending -> R.string.Sending
                                        else -> R.string.web3_message_request
                                    }
                                } else {
                                    when (step) {
                                        WalletConnectBottomSheetDialogFragment.Step.Loading -> R.string.web3_signing_confirmation
                                        WalletConnectBottomSheetDialogFragment.Step.Done -> R.string.web3_sending_success
                                        WalletConnectBottomSheetDialogFragment.Step.Error -> if (insufficientGas) R.string.insufficient_balance else R.string.web3_signing_failed
                                        WalletConnectBottomSheetDialogFragment.Step.Sending -> R.string.Sending
                                        else -> R.string.web3_signing_confirmation
                                    }
                                },
                        ),
                    style =
                        TextStyle(
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W600,
                        ),
                )
                Box(modifier = Modifier.height(8.dp))
                val clipboardManager = LocalClipboardManager.current
                val haptics = LocalHapticFeedback.current
                Text(
                    modifier =
                    Modifier
                        .padding(horizontal = 24.dp)
                        .combinedClickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            enabled = !errorInfo.isNullOrBlank(),
                            onClick = {},
                            onLongClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                clipboardManager.setText(AnnotatedString(errorInfo ?: "Nothing"))
                                toast(R.string.copied_to_clipboard)
                            },
                            onLongClickLabel = stringResource(id = R.string.copied_to_clipboard)
                        ),
                    text =
                        errorInfo ?: stringResource(
                            id =
                                if (step == WalletConnectBottomSheetDialogFragment.Step.Done) {
                                    if (type == JsSignMessage.TYPE_TRANSACTION) {
                                        R.string.web3_signing_transaction_success
                                    } else {
                                        R.string.web3_signing_message_success
                                    }
                                } else {
                                    R.string.web3_ensure_trust
                                },
                        ),
                    textAlign = TextAlign.Center,
                    style =
                        TextStyle(
                            color = if (errorInfo != null) MixinAppTheme.colors.tipError else MixinAppTheme.colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W400,
                        ),
                    maxLines = 3,
                    minLines = 3,
                )
                Box(modifier = Modifier.height(10.dp))
                Box(
                    modifier =
                    Modifier
                        .height(10.dp)
                        .fillMaxWidth()
                        .background(MixinAppTheme.colors.backgroundWindow),
                )
                if (JsSignMessage.isSignMessage(type)) {
                    MessagePreview(content = data ?: "") {
                        onPreviewMessage.invoke(it)
                    }
                } else if (chain == Chain.Solana) {
                    SolanaParsedTxPreview(parsedTx = parsedTx, asset = asset, solanaTxSource = solanaTxSource)
                } else if (token != null && amount != null) {
                    TokenTransactionPreview(amount = amount, token = token)
                } else {
                    TransactionPreview(
                        balance =
                            Convert.fromWei(
                                Numeric.decodeQuantity(transaction?.value ?: "0").toBigDecimal(),
                                Convert.Unit.ETHER,
                            ),
                        chain,
                        asset,
                    )
                }
                Box(modifier = Modifier.height(10.dp))
                val fee = tipGas?.displayValue(transaction?.maxFeePerGas) ?: solanaFee?.stripTrailingZeros() ?: BigDecimal.ZERO
                if (fee == BigDecimal.ZERO) {
                    FeeInfo(
                        amount = "$fee",
                        fee = fee.multiply(asset.priceUSD()),
                    )
                } else {
                    FeeInfo(
                        amount = "$fee ${asset?.symbol ?: ""}",
                        fee = fee.multiply(asset.priceUSD()),
                        gasPrice = tipGas?.displayGas(transaction?.maxFeePerGas)?.toPlainString(),
                    )
                }
                if (url != null && title != null) {
                    Box(modifier = Modifier.height(20.dp))
                    ItemContent(title = stringResource(id = R.string.From).uppercase(), subTitle = title, footer = url)
                }
                if (toAddress != null) {
                    Box(modifier = Modifier.height(20.dp))
                    ItemContent(title = stringResource(id = R.string.Receivers).uppercase(), subTitle = toAddress)
                }
                Box(modifier = Modifier.height(20.dp))
                ItemContent(title = stringResource(id = R.string.Account).uppercase(), subTitle = account)
                Box(modifier = Modifier.height(20.dp))
                ItemContent(title = stringResource(id = R.string.network).uppercase(), subTitle = chain.name)
                Box(modifier = Modifier.height(20.dp))
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                if ((tipGas == null && data == null && step != WalletConnectBottomSheetDialogFragment.Step.Error) || step == WalletConnectBottomSheetDialogFragment.Step.Loading || step == WalletConnectBottomSheetDialogFragment.Step.Sending) {
                    Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                        Box(modifier = Modifier.height(80.dp))
                    }
                } else if (step == WalletConnectBottomSheetDialogFragment.Step.Done || step == WalletConnectBottomSheetDialogFragment.Step.Error) {
                    Row(
                        modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .background(MixinAppTheme.colors.background)
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Button(
                            onClick = onDismissRequest,
                            colors =
                                ButtonDefaults.outlinedButtonColors(
                                    backgroundColor = MixinAppTheme.colors.accent,
                                ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 36.dp, vertical = 11.dp),
                        ) {
                            Text(text = stringResource(id = if (step == WalletConnectBottomSheetDialogFragment.Step.Done) R.string.Done else R.string.Got_it), color = Color.White)
                        }
                    }
                } else {
                    ActionBottom(
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter),
                        cancelTitle = stringResource(R.string.Cancel),
                        confirmTitle = stringResource(id = R.string.Continue),
                        cancelAction = onRejectAction,
                        confirmAction = showPin,
                    )
                }
                if (token == null && type == JsSignMessage.TYPE_TRANSACTION && (transaction?.value == null || Numeric.decodeQuantity(transaction.value) == BigInteger.ZERO)) {
                    Warning(modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
            Box(modifier = Modifier.height(16.dp))
        }
    }
}
