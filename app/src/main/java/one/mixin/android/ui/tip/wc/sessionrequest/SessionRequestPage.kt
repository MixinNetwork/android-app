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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.gson.Gson
import com.reown.walletkit.client.Wallet
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.composeDp
import one.mixin.android.extension.currencyFormat
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.numberFormat12
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.Method
import one.mixin.android.tip.wc.internal.TipGas
import one.mixin.android.tip.wc.internal.WCEthereumSignMessage
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.ui.home.web3.components.ActionBottom
import one.mixin.android.ui.home.web3.components.ActionButton
import one.mixin.android.ui.home.web3.components.MessagePreview
import one.mixin.android.ui.home.web3.components.TransactionPreview
import one.mixin.android.ui.home.web3.components.Warning
import one.mixin.android.ui.tip.wc.WalletConnectBottomSheetDialogFragment
import one.mixin.android.ui.tip.wc.compose.ItemContent
import one.mixin.android.ui.tip.wc.compose.Loading
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.ui.wallet.components.WalletLabel
import one.mixin.android.vo.priceUSD
import one.mixin.android.vo.safe.Token
import one.mixin.android.web3.js.Web3Signer
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
    isFeeWaived: Boolean = false,
    onFreeClick: (() -> Unit)? = null,
    onPreviewMessage: (String) -> Unit,
    onDismissRequest: () -> Unit,
    showPin: () -> Unit,
) {
    val viewModel = hiltViewModel<SessionRequestViewModel>()
    val context = LocalContext.current
    var walletName by remember { mutableStateOf<String?>(null) }
    val walletViewModel = hiltViewModel<WalletViewModel>()
    var walletDisplayInfo by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var chainToken by remember { mutableStateOf<Web3TokenItem?>(null) }

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
        } else if (sessionRequestUI.data is WCEthereumTransaction && (sessionRequestUI.data.value == null || Numeric.decodeQuantity(sessionRequestUI.data.value) == BigInteger.ZERO)) {
            2
        } else {
            1
        }

    LaunchedEffect(account) {
        try {
            walletDisplayInfo = walletViewModel.checkAddressAndGetDisplayName(account,null)
        } catch (e: Exception) {
            walletDisplayInfo = null
        }
    }


    LaunchedEffect(Unit) {
        try {
            val wallet = viewModel.findWalletById(Web3Signer.currentWalletId)
            walletName = wallet?.name?.takeIf { it.isNotEmpty() } ?: context.getString(R.string.Common_Wallet)
        } catch (e: Exception) {
            walletName = context.getString(R.string.Common_Wallet)
        }
    }

    LaunchedEffect(Unit) {
        try {
            chainToken = viewModel.web3TokenItemById(Web3Signer.currentWalletId, assetId = chain.assetId)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }


    val fee = tipGas?.displayValue(
        if (sessionRequestUI.data is WCEthereumTransaction) {
            sessionRequestUI.data.maxFeePerGas
        } else {
            null
        }
    ) ?: signData?.solanaFee?.stripTrailingZeros() ?: BigDecimal.ZERO

    MixinAppTheme {
        Column(
            modifier =
            Modifier
                .clip(shape = RoundedCornerShape(topStart = 8.composeDp, topEnd = 8.composeDp))
                .fillMaxWidth()
                .fillMaxHeight()
                .background(MixinAppTheme.colors.background),
        ) {
            WalletLabel(
                walletName = walletName,
                isWeb3 = true
            )
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
                        CoilImage(
                            sessionRequestUI.peerUI.icon,
                            modifier =
                            Modifier
                                .size(70.dp)
                                .clip(CircleShape),
                            placeholder = R.drawable.ic_avatar_place_holder,
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
                                        Numeric.decodeQuantity(
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

                if (fee == BigDecimal.ZERO) {
                    FeeInfo(
                        amount = "$fee",
                        fee = fee.multiply(asset.priceUSD()),
                        isFree = isFeeWaived,
                        onFreeClick = onFreeClick,
                    )
                } else {
                    FeeInfo(
                        amount = "$fee ${asset?.symbol}",
                        fee = fee.multiply(asset.priceUSD()),
                        gasPrice = tipGas?.displayGas(
                            if (sessionRequestUI.data is WCEthereumTransaction) {
                                sessionRequestUI.data.maxFeePerGas
                            } else {
                                null
                            }
                        )?.numberFormat12(),
                        isFree = isFeeWaived,
                        onFreeClick = onFreeClick,
                    )
                }
                Box(modifier = Modifier.height(20.dp))
                ItemContent(title = stringResource(id = R.string.From).uppercase(), subTitle = sessionRequestUI.peerUI.name, footer = sessionRequestUI.peerUI.uri)
                Box(modifier = Modifier.height(20.dp))
                walletDisplayInfo.notNullWithElse({ walletDisplayInfo ->
                    val (displayName, _) = walletDisplayInfo
                    ItemContent(title = stringResource(id = R.string.Wallet).uppercase(), subTitle = account, displayName)
                }, {
                    ItemContent(title = stringResource(id = R.string.Wallet).uppercase(), subTitle = account)
                })
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
                        if (fee != null && fee > BigDecimal.ZERO && (chainToken?.balance?.toBigDecimalOrNull() ?: BigDecimal.ZERO) <= BigDecimal.ZERO) {
                            Row(
                                modifier =
                                    Modifier
                                        .background(MixinAppTheme.colors.background)
                                        .padding(8.dp)
                                        .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                ActionButton(
                                    text = stringResource(id = R.string.insufficient_balance_symbol, chain.symbol),
                                    onClick = {
                                        viewModel.rejectRequest(version, topic)
                                        onDismissRequest.invoke()
                                    },
                                    backgroundColor = MixinAppTheme.colors.backgroundGray,
                                    contentColor = MixinAppTheme.colors.textPrimary
                                )
                                Box(modifier = Modifier.width(36.dp))
                            }
                        } else {
                            ActionBottom(modifier = Modifier.align(Alignment.BottomCenter), stringResource(id = R.string.Cancel), stringResource(id = R.string.Confirm), {
                                viewModel.rejectRequest(version, topic)
                                onDismissRequest.invoke()
                            }, showPin)
                        }
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
                color = MixinAppTheme.colors.textAssist,
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
    gasPrice: String? = null,
    isFree: Boolean = false,
    onFreeClick: (() -> Unit)? = null,
) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = stringResource(id = R.string.network_fee).uppercase(),
            color = MixinAppTheme.colors.textRemarks,
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
                    text = amount + if (gasPrice != null) " ($gasPrice Gwei)" else "",
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    style = TextStyle(textDecoration = if (isFree) TextDecoration.LineThrough else TextDecoration.None),
                )
                Box(modifier = Modifier.height(4.dp))
                Text(
                    text = fee.currencyFormat(),
                    color = MixinAppTheme.colors.textAssist,
                    fontSize = 14.sp,
                )
            }
            if (isFree) {
                Text(
                    text = stringResource(id = R.string.FREE),
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MixinAppTheme.colors.accent)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .let { m -> if (onFreeClick != null) m.clickable { onFreeClick.invoke() } else m }
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
