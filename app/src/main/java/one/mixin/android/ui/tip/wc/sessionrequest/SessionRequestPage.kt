package one.mixin.android.ui.tip.wc.sessionrequest

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.google.gson.Gson
import com.walletconnect.web3.wallet.client.Wallet
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.currencyFormat
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.Method
import one.mixin.android.tip.wc.internal.TipGas
import one.mixin.android.tip.wc.internal.WCEthereumSignMessage
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.tip.wc.internal.displayValue
import one.mixin.android.ui.home.web3.components.ActionBottom
import one.mixin.android.ui.home.web3.components.MessagePreview
import one.mixin.android.ui.home.web3.components.TransactionPreview
import one.mixin.android.ui.home.web3.components.Warning
import one.mixin.android.ui.tip.wc.WalletConnectBottomSheetDialogFragment
import one.mixin.android.ui.tip.wc.compose.ItemContent
import one.mixin.android.ui.tip.wc.compose.Loading
import one.mixin.android.vo.priceUSD
import one.mixin.android.vo.safe.Token
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

@Composable
fun SessionRequestPage(
    gson: Gson,
    version: WalletConnect.Version,
    account: String,
    step: WalletConnectBottomSheetDialogFragment.Step,
    chain: Chain,
    topic: String,
    sessionRequest: Wallet.Model.SessionRequest?,
    signData: WalletConnect.WCSignData.V2SignData<*>?,
    asset: Token?,
    tipGas: TipGas?,
    errorInfo: String?,
    onPreviewMessage: (String) -> Unit,
    onDismissRequest: () -> Unit,
    showPin: () -> Unit,
) {
    val viewModel = hiltViewModel<SessionRequestViewModel>()
    if (version != WalletConnect.Version.TIP && (signData == null || sessionRequest == null)) {
        Loading()
        return
    }
    val sessionRequestUI = viewModel.getSessionRequestUI(version, chain, signData, sessionRequest)
    if (sessionRequestUI == null) {
        Loading()
        return
    }
    val signType =
        if ((sessionRequestUI.data as? WCEthereumSignMessage)?.type == WCEthereumSignMessage.WCSignType.PERSONAL_MESSAGE) {
            0
        } else if (sessionRequestUI.data is WCEthereumTransaction && (sessionRequestUI.data.value == null || Numeric.toBigInt(sessionRequestUI.data.value) == BigInteger.ZERO)) {
            2
        } else {
            1
        }

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
                        .weight(weight = 1f, fill = false),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.height(50.dp))
                when (step) {
                    WalletConnectBottomSheetDialogFragment.Step.Loading, WalletConnectBottomSheetDialogFragment.Step.Sending -> {
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

                    else ->
                        AsyncImage(
                            model =
                                ImageRequest.Builder(LocalContext.current)
                                    .data(sessionRequestUI.peerUI.icon)
                                    .decoderFactory(SvgDecoder.Factory())
                                    .build(),
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(70.dp)
                                    .clip(CircleShape),
                            placeholder = painterResource(id = R.drawable.ic_avatar_place_holder),
                        )
                }
                Box(modifier = Modifier.height(16.dp))
                Text(
                    text =
                        stringResource(
                            id =
                                if (sessionRequestUI.data is WCEthereumSignMessage) {
                                    if (signType == 0) {
                                        when (step) {
                                            WalletConnectBottomSheetDialogFragment.Step.Loading -> R.string.web3_message_request
                                            WalletConnectBottomSheetDialogFragment.Step.Done -> R.string.web3_sending_success
                                            WalletConnectBottomSheetDialogFragment.Step.Error -> R.string.web3_signing_failed
                                            WalletConnectBottomSheetDialogFragment.Step.Sending -> R.string.Sending
                                            else -> R.string.web3_message_request
                                        }
                                    } else {
                                        when (step) {
                                            WalletConnectBottomSheetDialogFragment.Step.Loading -> R.string.web3_transaction_request
                                            WalletConnectBottomSheetDialogFragment.Step.Done -> R.string.web3_sending_success
                                            WalletConnectBottomSheetDialogFragment.Step.Error -> R.string.web3_signing_failed
                                            WalletConnectBottomSheetDialogFragment.Step.Sending -> R.string.Sending
                                            else -> R.string.web3_transaction_request
                                        }
                                    }
                                } else {
                                    when (step) {
                                        WalletConnectBottomSheetDialogFragment.Step.Loading -> R.string.web3_signing_confirmation
                                        WalletConnectBottomSheetDialogFragment.Step.Done -> R.string.web3_sending_success
                                        WalletConnectBottomSheetDialogFragment.Step.Error -> R.string.web3_signing_failed
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
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text =
                        errorInfo ?: stringResource(
                            id =
                                if (step == WalletConnectBottomSheetDialogFragment.Step.Done) {
                                    if (sessionRequestUI.data is WCEthereumSignMessage) {
                                        if (signType == 0) {
                                            R.string.web3_signing_transaction_success
                                        } else {
                                            R.string.web3_signing_transaction_success
                                        }
                                    } else {
                                        R.string.web3_signing_transaction_success
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
                Box(modifier = Modifier.height(20.dp))
                Box(
                    modifier =
                        Modifier
                            .height(10.dp)
                            .fillMaxWidth()
                            .background(MixinAppTheme.colors.backgroundWindow),
                )
                when (sessionRequestUI.data) {
                    is WCEthereumTransaction -> {
                        if (signType == 2) {
                            MessagePreview(content = viewModel.getContent(version, gson, sessionRequestUI.data)) {
                                onPreviewMessage.invoke(it)
                            }
                        } else {
                            TransactionPreview(
                                balance =
                                    Convert.fromWei(
                                        Numeric.toBigInt(
                                            sessionRequestUI.data.value ?: "0",
                                        ).toBigDecimal(),
                                        Convert.Unit.ETHER,
                                    ),
                                sessionRequestUI.chain,
                                asset,
                            )
                        }
                    }

                    else -> {
                        MessagePreview(content = viewModel.getContent(version, gson, sessionRequestUI.data)) {
                            onPreviewMessage.invoke(it)
                        }
                    }
                }
                Box(modifier = Modifier.height(20.dp))

                val fee = tipGas?.displayValue() ?: signData?.solanaFee?.stripTrailingZeros() ?: BigDecimal.ZERO
                if (fee == BigDecimal.ZERO) {
                    FeeInfo(
                        amount = "$fee",
                        fee = fee.multiply(asset.priceUSD()),
                    )
                } else {
                    FeeInfo(
                        amount = "$fee ${asset?.symbol}",
                        fee = fee.multiply(asset.priceUSD()),
                    )
                }
                Box(modifier = Modifier.height(20.dp))
                ItemContent(title = stringResource(id = R.string.From).uppercase(), subTitle = sessionRequestUI.peerUI.name, footer = sessionRequestUI.peerUI.uri)
                Box(modifier = Modifier.height(20.dp))
                ItemContent(title = stringResource(id = R.string.Account).uppercase(), subTitle = account)
                Box(modifier = Modifier.height(20.dp))
                ItemContent(title = stringResource(id = R.string.network).uppercase(), subTitle = chain.name)
                Box(
                    modifier =
                        Modifier
                            .weight(1f),
                )
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(),
            ) {
                if (step == WalletConnectBottomSheetDialogFragment.Step.Done || step == WalletConnectBottomSheetDialogFragment.Step.Error) {
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
                            Text(text = stringResource(id = R.string.Done), color = Color.White)
                        }
                    }
                } else if (step == WalletConnectBottomSheetDialogFragment.Step.Sign) {
                    if (signData?.sessionRequest?.request?.method == Method.ETHSignTransaction.name || signData?.sessionRequest?.request?.method == Method.ETHSendTransaction.name && tipGas == null) {
                        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                            Box(modifier = Modifier.height(20.dp))
                            CircularProgressIndicator(
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .align(Alignment.CenterHorizontally),
                                color = MixinAppTheme.colors.accent,
                            )
                        }
                    } else {
                        ActionBottom(modifier = Modifier.align(Alignment.BottomCenter), stringResource(id = R.string.Cancel), stringResource(id = R.string.Confirm), {
                            viewModel.rejectRequest(version, topic)
                            onDismissRequest.invoke()
                        }, showPin)
                    }
                }

                if (signType == 2) Warning(modifier = Modifier.align(Alignment.BottomCenter))
            }
            Box(modifier = Modifier.height(32.dp))
        }

        Timber.e("Step $step")
    }
}

private enum class Hint {
    NoPreview,
    Cancel,
    SpeedUp,
}

@Composable
private fun Hint(hint: Hint) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MixinAppTheme.colors.backgroundWindow)
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter =
                painterResource(
                    when (hint) {
                        Hint.NoPreview -> R.drawable.ic_warning
                        Hint.Cancel -> R.drawable.ic_transaction_cancel
                        Hint.SpeedUp -> R.drawable.ic_transaction_speed
                    },
                ),
            modifier =
                Modifier
                    .size(40.dp, 40.dp)
                    .padding(horizontal = 8.dp),
            contentDescription = null,
        )
        Box(modifier = Modifier.width(8.dp))
        Column(
            modifier =
                Modifier
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                modifier = Modifier.padding(top = 16.dp),
                text =
                    stringResource(
                        id =
                            when (hint) {
                                Hint.NoPreview -> R.string.preview_unavailable
                                Hint.Cancel -> R.string.Cancel_transaction
                                Hint.SpeedUp -> R.string.Speed_up_transaction
                            },
                    ),
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 14.sp,
            )
            Text(
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
                text =
                    stringResource(
                        id =
                            when (hint) {
                                Hint.NoPreview -> R.string.preview_unavailable_description
                                Hint.Cancel -> R.string.cancel_transaction_tip
                                Hint.SpeedUp -> R.string.speed_up_transaction_tip
                            },
                    ),
                color = MixinAppTheme.colors.textSubtitle,
                fontSize = 14.sp,
            )
        }
        Box(modifier = Modifier.width(16.dp))
    }
}

@Composable
fun FeeInfo(
    amount: String,
    fee: BigDecimal,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
    ) {
        Text(
            text = stringResource(id = R.string.network_fee).uppercase(),
            color = MixinAppTheme.colors.textSubtitle,
            fontSize = 14.sp,
        )
        Box(modifier = Modifier.height(4.dp))
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = amount,
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 14.sp,
                )
                Box(modifier = Modifier.height(4.dp))
                Text(
                    text = fee.currencyFormat(),
                    color = MixinAppTheme.colors.textSubtitle,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Preview
@Composable
private fun NetworkInfoPreview() {
    FeeInfo("0.0169028 ETH", BigDecimal("7.57"))
}

@Preview
@Composable
private fun HintPreview() {
    Column(modifier = Modifier.padding(8.dp)) {
        Hint(Hint.NoPreview)
        Box(modifier = Modifier.height(8.dp))
        Hint(Hint.Cancel)
        Box(modifier = Modifier.height(8.dp))
        Hint(Hint.SpeedUp)
    }
}
