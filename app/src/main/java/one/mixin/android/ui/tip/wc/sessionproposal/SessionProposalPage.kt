package one.mixin.android.ui.tip.wc.sessionproposal

import GlideImage
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.walletconnect.web3.wallet.client.Wallet
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.ui.tip.wc.WalletConnectBottomSheetDialogFragment
import one.mixin.android.ui.tip.wc.compose.ItemContent
import one.mixin.android.ui.tip.wc.connections.Loading
import one.mixin.android.ui.tip.wc.sessionrequest.TransferBottom
import timber.log.Timber

@Composable
fun SessionProposalPage(
    version: WalletConnect.Version,
    account: String,
    step: WalletConnectBottomSheetDialogFragment.Step,
    chain: Chain,
    topic: String,
    sessionProposal: Wallet.Model.SessionProposal?,
    errorInfo: String?,
    onDismissRequest: () -> Unit,
    showPin: () -> Unit,
) {
    val viewModel = hiltViewModel<SessionProposalViewModel>()
    if (version != WalletConnect.Version.TIP && sessionProposal == null) {
        Loading()
        return
    }

    val sessionProposalUI = viewModel.getSessionProposalUI(version, chain, sessionProposal)
    if (sessionProposalUI == null) {
        Loading()
        return
    }
    val chainName = sessionProposalUI.chain.name
    MixinAppTheme {
        Column(
            modifier =
            Modifier
                .clip(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .fillMaxWidth()
                .fillMaxHeight()
                .background(MixinAppTheme.colors.background),
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
                else -> {
                    GlideImage(
                        data = sessionProposalUI.peer.icon,
                        modifier =
                        Modifier
                            .size(70.dp)
                            .clip(CircleShape),
                        placeHolderPainter = painterResource(id = R.drawable.ic_avatar_place_holder),
                    )
                }
            }
            Box(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = if (step == WalletConnectBottomSheetDialogFragment.Step.Done) R.string.link_succeeded  else if(step == WalletConnectBottomSheetDialogFragment.Step.Error) R.string.link_failed else R.string.connect_wallet),
                style =
                TextStyle(
                    color = MixinAppTheme.colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W500,
                ),

            )
            Box(modifier = Modifier.height(8.dp))
            Text(
                text = errorInfo ?: stringResource(id = R.string.allow_dapp_access_address_and_transaction),
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
            Box(modifier = Modifier.height(20.dp))
            ItemContent(title = stringResource(id = R.string.From).uppercase(), subTitle = sessionProposalUI.peer.name, footer = sessionProposalUI.peer.uri)
            Box(modifier = Modifier.height(20.dp))
            ItemContent(title = stringResource(id = R.string.Account).uppercase(), subTitle = account)
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
            } else if (step != WalletConnectBottomSheetDialogFragment.Step.Loading){
                TransferBottom(stringResource(id = R.string.Cancel), stringResource(id = R.string.Confirm), {
                    viewModel.rejectSession(version, topic)
                    onDismissRequest.invoke()
                }, showPin)
            }
            Box(
                modifier = Modifier.height(32.dp)
            )
        }
    }
}

