package one.mixin.android.ui.tip.wc.sessionrequest

import GlideImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.Gson
import com.walletconnect.web3.wallet.client.Wallet
import one.mixin.android.R
import one.mixin.android.api.response.GasPriceType
import one.mixin.android.api.response.TipGas
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.WCEthereumSignMessage
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.ui.setting.ui.compose.MixinBottomSheetDialog
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.ui.tip.wc.WalletConnectBottomSheetDialogFragment
import one.mixin.android.ui.tip.wc.compose.ItemContent
import one.mixin.android.ui.tip.wc.compose.Loading
import one.mixin.android.vo.priceUSD
import one.mixin.android.vo.safe.Token
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigDecimal

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
    gasPriceType: GasPriceType,
    errorInfo: String?,
    onPreviewMessage: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onPositiveClick: (Long?) -> Unit,
    showPin: () -> Unit,
    onGasItemClick: (GasPriceType) -> Unit,
) {
    Timber.e("step $step")
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
    val isEthSign = (sessionRequestUI.data as? WCEthereumSignMessage)?.type == WCEthereumSignMessage.WCSignType.MESSAGE
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }

    MixinAppTheme {
        Column(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .fillMaxWidth()
                .fillMaxHeight()
                .background(MixinAppTheme.colors.background),
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
                        tint = Color.Unspecified
                    )
                }

                WalletConnectBottomSheetDialogFragment.Step.Done -> {
                    Icon(
                        modifier = Modifier.size(70.dp),
                        painter = painterResource(id = R.drawable.ic_transfer_status_success),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }

                else ->
                    GlideImage(
                        data = sessionRequestUI.peerUI.icon,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape),
                        placeHolderPainter = painterResource(id = R.drawable.ic_avatar_place_holder),
                    )
            }
            Box(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(
                    id = if (sessionRequestUI.data is WCEthereumSignMessage) {
                        when (step) {
                            WalletConnectBottomSheetDialogFragment.Step.Loading -> R.string.web3_signing
                            WalletConnectBottomSheetDialogFragment.Step.Done -> R.string.web3_signing_success
                            WalletConnectBottomSheetDialogFragment.Step.Error -> R.string.web3_signing_failed
                            WalletConnectBottomSheetDialogFragment.Step.Send -> R.string.signature_request
                            WalletConnectBottomSheetDialogFragment.Step.Sending -> R.string.Sending
                            else -> R.string.signature_request

                        }
                    } else {
                        when (step) {
                            WalletConnectBottomSheetDialogFragment.Step.Loading -> R.string.web3_signing
                            WalletConnectBottomSheetDialogFragment.Step.Done -> R.string.web3_signing_success
                            WalletConnectBottomSheetDialogFragment.Step.Error -> R.string.web3_signing_failed
                            WalletConnectBottomSheetDialogFragment.Step.Send -> R.string.transaction_request
                            WalletConnectBottomSheetDialogFragment.Step.Sending -> R.string.Sending
                            else -> R.string.transaction_request
                        }
                    }
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
                modifier = Modifier.padding(horizontal = 16.dp),
                text = errorInfo ?: stringResource(id = R.string.allow_dapp_access_address_and_transaction),
                textAlign = TextAlign.Center,
                style =
                TextStyle(
                    color = if (errorInfo != null) MixinAppTheme.colors.tipError else MixinAppTheme.colors.textSubtitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W400,
                ),
                maxLines = 3,
                minLines = 3,
            )
            Box(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .height(10.dp)
                    .fillMaxWidth()
                    .background(MixinAppTheme.colors.backgroundWindow)
            )
            when (sessionRequestUI.data) {
                is WCEthereumTransaction -> {
                    Transaction(
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

                else -> {
                    Message(content = viewModel.getContent(version, gson, sessionRequestUI.data)) {
                        onPreviewMessage.invoke(it)
                    }
                }
            }
            Box(modifier = Modifier.height(20.dp))
            val fee = if (tipGas == null) BigDecimal.ZERO else gasPriceType.calcGas(tipGas)
            if (fee == BigDecimal.ZERO) {
                FeeInfo(
                    amount = "$fee",
                    fee = (if (tipGas == null) BigDecimal.ZERO else gasPriceType.calcGas(tipGas)).multiply(asset.priceUSD()).toPlainString(),
                )
            } else {
                FeeInfo(
                    amount = "$fee ${asset?.symbol}",
                    fee = (if (tipGas == null) BigDecimal.ZERO else gasPriceType.calcGas(tipGas)).multiply(asset.priceUSD()).toPlainString(),
                ) {
                    openBottomSheet = true
                }
            }
            Box(modifier = Modifier.height(20.dp))
            ItemContent(title = stringResource(id = R.string.From).uppercase(), subTitle = sessionRequestUI.peerUI.name, footer = sessionRequestUI.peerUI.uri)
            Box(modifier = Modifier.height(20.dp))
            ItemContent(title = stringResource(id = R.string.Account).uppercase(), subTitle = account)
            Box(modifier = Modifier.height(20.dp))
            ItemContent(title = stringResource(id = R.string.network).uppercase(), subTitle = chain.name)
            Box(
                modifier = Modifier
                    .weight(1f)
            )
            if (step == WalletConnectBottomSheetDialogFragment.Step.Done || step == WalletConnectBottomSheetDialogFragment.Step.Error) {
                Row(
                    modifier = Modifier
                        .background(MixinAppTheme.colors.background)
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.outlinedButtonColors(
                            backgroundColor = MixinAppTheme.colors.accent,
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 36.dp, vertical = 11.dp),
                    ) {
                        Text(text = stringResource(id = R.string.Done), color = Color.White)
                    }
                }
            } else if (step == WalletConnectBottomSheetDialogFragment.Step.Sign) {
                TransferBottom(stringResource(id = R.string.Cancel), stringResource(id = R.string.Confirm), {
                    viewModel.rejectRequest(version, topic)
                    onDismissRequest.invoke()
                }, showPin)
            } else if (step == WalletConnectBottomSheetDialogFragment.Step.Send) {
                TransferBottom(stringResource(id = R.string.Discard), stringResource(id = R.string.Send), {
                    viewModel.rejectRequest(version, topic)
                    onDismissRequest.invoke()
                }, { onPositiveClick.invoke(sessionRequestUI.requestId) })
            }
            Box(modifier = Modifier.height(32.dp))
        }

        Timber.e("Step $step")

        if (openBottomSheet && tipGas != null && asset != null) {
            ChooseGasBottomSheet(
                tipGas,
                asset,
                onDismissRequest = { openBottomSheet = false },
                onItemClick = { gasPriceType ->
                    openBottomSheet = false
                    onGasItemClick.invoke(gasPriceType)
                },
            )
        }
    }
}

@Composable
private fun Transaction(
    balance: BigDecimal,
    chain: Chain,
    asset: Token?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(MixinAppTheme.colors.background)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Box(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.Balance_Change),
            color = MixinAppTheme.colors.textSubtitle,
            fontSize = 14.sp,
        )
        Box(modifier = Modifier.height(8.dp))
        Row(
            modifier =
            Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                modifier = Modifier.alignByBaseline(),
                text = "-$balance",
                color = Color(0xFFE86B67),
                fontFamily = FontFamily(Font(R.font.mixin_font)),
                fontSize = 30.sp,
            )
            Box(modifier = Modifier.width(4.dp))
            Text(
                modifier = Modifier.alignByBaseline(),
                text = chain.symbol,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 12.sp,
            )
            Box(modifier = Modifier.weight(1f))
            GlideImage(
                data = asset?.iconUrl ?: "",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                placeHolderPainter = painterResource(id = R.drawable.ic_avatar_place_holder),
            )
        }
        Text(
            text = "≈ $${balance.multiply(asset.priceUSD()).toPlainString()}",
            color = MixinAppTheme.colors.textMinor,
            fontSize = 12.sp,
        )
        Box(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun WarningPreview() {
    Warning(false)
}

@Composable
private fun Message(
    content: String,
    onPreviewMessage: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Box(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(id = R.string.Message),
            color = MixinAppTheme.colors.textSubtitle,
            fontSize = 14.sp,
        )
        Box(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(0.dp, 128.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MixinAppTheme.colors.backgroundWindow)
                .clickable { onPreviewMessage(content) },
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                text = content,
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 16.sp,
            )
            Image(
                painter = painterResource(R.drawable.ic_post),
                modifier = Modifier
                    .size(40.dp, 40.dp)
                    .padding(horizontal = 8.dp)
                    .align(Alignment.TopEnd),
                contentDescription = null,
            )
        }
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
        modifier = Modifier
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
            modifier = Modifier
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
private fun FeeInfo(
    amount: String,
    fee: String,
    onFeeClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
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
            modifier = Modifier
                .clickable { onFeeClick?.invoke() }
                .fillMaxWidth(),
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
                    text = "≈ $$fee",
                    color = MixinAppTheme.colors.textSubtitle,
                    fontSize = 14.sp,
                )
            }
            if (onFeeClick != null)
                Image(
                    painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                    modifier =
                    Modifier
                        .size(20.dp, 20.dp),
                    contentDescription = null,
                )
        }
    }
}

@Composable
private fun Warning(isEthSign: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MixinAppTheme.colors.tipWarning)
            .border(1.dp, MixinAppTheme.colors.tipWarningBorder)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_warning),
            modifier = Modifier
                .size(40.dp, 40.dp)
                .padding(horizontal = 8.dp),
            contentDescription = null,
        )
        Box(modifier = Modifier.width(8.dp))
        Text(
            modifier = Modifier.padding(vertical = 12.dp),
            text = if (isEthSign) stringResource(id = R.string.blocked_action, "eth_sign") else stringResource(id = R.string.signature_request_warning),
            color = if (isEthSign) MixinAppTheme.colors.red else MixinAppTheme.colors.textPrimary,
            fontSize = 14.sp,
        )
        Box(modifier = Modifier.width(16.dp))
    }
}

@Composable
private fun ChooseGasBottomSheet(
    tipGas: TipGas,
    asset: Token,
    onDismissRequest: () -> Unit,
    onItemClick: (GasPriceType) -> Unit,
) {
    MixinBottomSheetDialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(MixinAppTheme.colors.background),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    modifier = Modifier.padding(start = 20.dp),
                    text = stringResource(id = R.string.network_fee),
                    fontSize = 18.sp,
                    fontWeight = FontWeight(600),
                    color = MixinAppTheme.colors.textPrimary,
                )
                Image(
                    painter = painterResource(R.drawable.ic_circle_close),
                    modifier = Modifier
                        .size(52.dp, 52.dp)
                        .clip(CircleShape)
                        .clickable(onClick = {
                            onDismissRequest.invoke()
                        })
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    contentDescription = null,
                )
            }
            GasItem(gasPriceType = GasPriceType.Fast, tipGas = tipGas, asset = asset, onItemClick)
            GasItem(gasPriceType = GasPriceType.Propose, tipGas = tipGas, asset = asset, onItemClick)
            GasItem(gasPriceType = GasPriceType.Safe, tipGas = tipGas, asset = asset, onItemClick)
            Box(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview
@Composable
private fun NetworkInfoPreview() {
    FeeInfo("0.0169028 ETH", "$7.57") {}
}

@Composable
private fun GasItem(
    gasPriceType: GasPriceType,
    tipGas: TipGas,
    asset: Token,
    onItemClick: (GasPriceType) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(shape = RoundedCornerShape(13.dp))
            .fillMaxWidth()
            .background(MixinAppTheme.colors.backgroundWindow)
            .clickable(onClick = {
                onItemClick.invoke(gasPriceType)
            })
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(id = gasPriceType.getGasPriceName()),
                fontSize = 16.sp,
                color = MixinAppTheme.colors.textPrimary,
            )
            Text(
                text = "$${gasPriceType.calcGas(tipGas).multiply(asset.priceUSD()).toPlainString()}",
                fontSize = 16.sp,
                color = MixinAppTheme.colors.textPrimary,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = gasPriceType.getEstimateTime(),
                fontSize = 13.sp,
                color = MixinAppTheme.colors.textMinor,
            )
            Text(
                text = "${gasPriceType.calcGas(tipGas).toPlainString()} ${asset.symbol}",
                fontSize = 13.sp,
                color = MixinAppTheme.colors.textMinor,
            )
        }
    }
}

@Composable
fun TransferBottom(cancelTitle:String, confirmTitle:String,cancelAction: () -> Unit, confirmAction: () -> Unit) {
    Row(
        modifier = Modifier
            .background(MixinAppTheme.colors.background)
            .padding(20.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {

        Button(
            onClick = cancelAction,
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = MixinAppTheme.colors.backgroundGray,
            ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 36.dp, vertical = 11.dp),
        ) {
            Text(text = cancelTitle, color = MixinAppTheme.colors.textPrimary)
        }
        Box(modifier = Modifier.width(36.dp))
        Button(
            onClick = confirmAction,
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = MixinAppTheme.colors.accent,
            ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 36.dp, vertical = 11.dp),
        ) {
            Text(text = confirmTitle, color = Color.White)
        }
    }
}

@Preview
@Composable
fun Message() {
    Box(modifier = Modifier.background(MixinAppTheme.colors.background)) {
        Message(
            content = """{
          "raw": [
            "0x9df67f5a05fb594c4357d87221cbd69f1d5a6fbb",
            "{\"types\":{\"Alias\":[{\"name\":\"from\",\"type\":\"address\"},{\"name\":\"alias\",\"type\":\"address\"},{\"name\":\"timestamp\",\"type\":\"uint64\"}],\"EIP712Domain\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"version\",\"type\":\"string\"}]},\"domain\":{\"name\":\"snapshot\",\"version\":\"0.1.4\"},\"primaryType\":\"Alias\",\"message\":{\"from\":\"0x9df67f5a05fb594c4357d87221cbd69f1d5a6fbb\",\"alias\":\"0x8f14e8dbc7b3619e5210201022f637f271545c90\",\"timestamp\":\"1710766295\"}}"
          ],
          "type": "TYPED_MESSAGE"
        }
    """
        ) {

        }
    }
}

@Preview
@Composable
fun TransferBottomPreview() {
    Column {
        TransferBottom(stringResource(id = R.string.Cancel), stringResource(id = R.string.Confirm), {}, {})
        TransferBottom(stringResource(id = R.string.Discard), stringResource(id = R.string.Send), {}, {})
    }
}

@Preview
@Composable
private fun TransactionPreview() {
    Transaction(balance = BigDecimal(0.134), chain = Chain.Ethereum, null)
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

@Preview
@Composable
private fun GasItemPreview() {
    GasItem(
        gasPriceType = GasPriceType.Propose,
        tipGas = TipGas("43d61dcd-e413-450d-80b8-101d5e903357", "0.00000002", "0.0000003", "0.000005", "250000"),
        asset =
        Token(
            "c6d0c728-2624-429b-8e0d-d9d19b6592fa", "c6d0c728-2624-429b-8e0d-d9d19b6592fa", "BTC", "Bitcoin",
            "https://mixin-images.zeromesh.net/HvYGJsV5TGeZ-X9Ek3FEQohQZ3fE9LBEBGcOcn4c4BNHovP4fW4YB97Dg5LcXoQ1hUjMEgjbl1DPlKg1TW7kK6XP=s128",
            "", "1", "1", "30000", "30000", 3, "", "",
        ),
    ) {}
}

