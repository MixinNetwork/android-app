package one.mixin.android.ui.tip.wc.sessionproposal

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.reown.walletkit.client.Wallet
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.isImported
import one.mixin.android.extension.composeDp
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectV2.getProposalChainIds
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.ui.home.web3.components.ActionBottom
import one.mixin.android.ui.tip.wc.WalletConnectBottomSheetDialogFragment
import one.mixin.android.ui.tip.wc.compose.Loading
import one.mixin.android.web3.js.Web3Signer
import one.mixin.android.widget.components.MixinButton

@Composable
fun SessionProposalPage(
    version: WalletConnect.Version,
    account: String,
    step: WalletConnectBottomSheetDialogFragment.Step,
    chain: Chain,
    topic: String,
    sessionProposal: Wallet.Model.SessionProposal?,
    errorInfo: String?,
    selectedWalletId: String = Web3Signer.currentWalletId,
    isSigningBlocked: Boolean = false,
    isEligibilityChecked: Boolean = true,
    onSelectWalletClick: () -> Unit = {},
    onSwitchWallet: () -> Unit = {},
    onDismissRequest: () -> Unit,
    showPin: () -> Unit,
) {
    val viewModel = hiltViewModel<SessionProposalViewModel>()
    if (!isSigningBlocked && (!isEligibilityChecked || (version != WalletConnect.Version.TIP && sessionProposal == null))) {
        Loading()
        return
    }
    val commonWallet = stringResource(R.string.Common_Wallet)
    var walletDisplayData by remember { mutableStateOf<WalletConnectSelectableWallet?>(null) }

    val sessionProposalUI =
        if (isSigningBlocked && sessionProposal == null) {
            null
        } else {
            viewModel.getSessionProposalUI(version, chain, sessionProposal)
        }
    if (!isSigningBlocked && sessionProposalUI == null) {
        Loading()
        return
    }

    LaunchedEffect(selectedWalletId) {
        walletDisplayData =
            if (selectedWalletId.isBlank()) {
                null
            } else {
                viewModel.loadWalletDisplayData(selectedWalletId)
            }
    }

    val proposalChainIds = remember(sessionProposal) { sessionProposal?.getProposalChainIds() }
    val canSelectWallet =
        !isSigningBlocked &&
            version == WalletConnect.Version.V2 &&
            step == WalletConnectBottomSheetDialogFragment.Step.Input

    MixinAppTheme {
        Column(
            modifier =
                Modifier
                    .clip(shape = RoundedCornerShape(topStart = 8.composeDp, topEnd = 8.composeDp))
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(MixinAppTheme.colors.background),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(50.dp))

                when (step) {
                    WalletConnectBottomSheetDialogFragment.Step.Loading -> {
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
                        if (sessionProposalUI != null) {
                            CoilImage(
                                model = sessionProposalUI.peer.icon,
                                modifier =
                                    Modifier
                                        .size(70.dp)
                                        .clip(CircleShape),
                                placeholder = R.drawable.ic_avatar_place_holder,
                            )
                        } else {
                            Icon(
                                modifier = Modifier.size(70.dp),
                                painter = painterResource(id = R.drawable.ic_transfer_status_failed),
                                contentDescription = null,
                                tint = Color.Unspecified,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text =
                        stringResource(
                            id =
                                if (step == WalletConnectBottomSheetDialogFragment.Step.Done) {
                                    R.string.connect_wallet_success
                                } else if (step == WalletConnectBottomSheetDialogFragment.Step.Error) {
                                    R.string.connect_wallet_failed
                                } else {
                                    R.string.connect_wallet
                                },
                        ),
                    style =
                        TextStyle(
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.W500,
                        ),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = errorInfo ?: stringResource(id = R.string.allow_dapp_access_address_and_transaction),
                    style =
                        TextStyle(
                            color =
                                if (errorInfo != null || step == WalletConnectBottomSheetDialogFragment.Step.Error) {
                                    MixinAppTheme.colors.tipError
                                } else {
                                    MixinAppTheme.colors.textMinor
                                },
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            fontWeight = FontWeight.W400,
                        ),
                    textAlign = TextAlign.Center,
                    maxLines = if (isSigningBlocked) 5 else 3,
                    minLines = 3,
                )
                if (sessionProposalUI != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier =
                            Modifier
                                .height(6.dp)
                                .fillMaxWidth()
                                .background(MixinAppTheme.colors.backgroundWindow),
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.From).uppercase(),
                            color = MixinAppTheme.colors.textRemarks,
                            fontSize = 14.sp,
                            maxLines = 1,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = sessionProposalUI.peer.name,
                            color = MixinAppTheme.colors.textPrimary,
                            fontSize = 16.sp,
                        )
                        if (sessionProposalUI.peer.uri.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = sessionProposalUI.peer.uri,
                                color = MixinAppTheme.colors.textAssist,
                                fontSize = 14.sp,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.Wallet).uppercase(),
                            color = MixinAppTheme.colors.textRemarks,
                            fontSize = 14.sp,
                            maxLines = 1,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val wallet = walletDisplayData?.wallet
                        val displayName = wallet?.name?.takeIf { it.isNotBlank() } ?: commonWallet

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(
                                        enabled = canSelectWallet,
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = LocalIndication.current,
                                        onClick = onSelectWalletClick,
                                    ),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = displayName,
                                        color = MixinAppTheme.colors.textPrimary,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    if (wallet?.isImported() == true) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        WalletLabelBadge(
                                            text = stringResource(R.string.Imported),
                                            textColor = MixinAppTheme.colors.textRemarks,
                                            backgroundColor = MixinAppTheme.colors.backgroundGrayLight,
                                        )
                                    }
                                }
                                if (canSelectWallet) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier.size(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_web3_drop_down),
                                            contentDescription = null,
                                            tint = Color.Unspecified,
                                        )
                                    }
                                }
                            }

                            val currentWalletData = walletDisplayData
                            if (currentWalletData != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                WalletBalanceRow(balance = currentWalletData.balance)
                                Spacer(modifier = Modifier.height(12.dp))
                                WalletChainAddressRows(
                                    addresses = currentWalletData.addresses,
                                    proposalChainIds = proposalChainIds,
                                )
                            } else if (account.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = account,
                                    color = MixinAppTheme.colors.textAssist,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            if (step == WalletConnectBottomSheetDialogFragment.Step.Error && isSigningBlocked) {
                ActionBottom(
                    modifier = Modifier,
                    cancelTitle = stringResource(id = R.string.Cancel),
                    confirmTitle = stringResource(id = R.string.switch_wallet),
                    cancelAction = onDismissRequest,
                    confirmAction = onSwitchWallet,
                )
            } else if (step == WalletConnectBottomSheetDialogFragment.Step.Done || step == WalletConnectBottomSheetDialogFragment.Step.Error) {
                Row(
                    modifier =
                        Modifier
                            .background(MixinAppTheme.colors.background)
                            .padding(20.dp)
                            .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    MixinButton(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(30.dp),
                        contentPadding = PaddingValues(horizontal = 35.dp, vertical = 10.dp),
                    ) {
                        Text(text = stringResource(id = R.string.Done), fontSize = 16.sp, color = Color.White)
                    }
                }
            } else if (step != WalletConnectBottomSheetDialogFragment.Step.Loading) {
                ActionBottom(
                    modifier = Modifier,
                    cancelTitle = stringResource(id = R.string.Cancel),
                    confirmTitle = stringResource(id = R.string.Connect),
                    cancelAction = {
                        viewModel.rejectSession(version, topic)
                        onDismissRequest.invoke()
                    },
                    confirmAction = showPin,
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
