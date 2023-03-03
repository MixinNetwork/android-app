package one.mixin.android.ui.tip.wc.switchnetwork

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.ui.tip.wc.WalletConnectBottomSheetDialogFragment
import one.mixin.android.ui.tip.wc.connections.Loading
import one.mixin.android.ui.tip.wc.sessionproposal.DAppInfo
import one.mixin.android.ui.tip.wc.sessionproposal.SessionProposalViewModel
import one.mixin.android.ui.tip.wc.sessionproposal.WCPinBoard

@Composable
fun SwitchNetworkPage(
    version: WalletConnect.Version,
    step: WalletConnectBottomSheetDialogFragment.Step,
    onDismissRequest: () -> Unit,
    onBiometricClick: (() -> Unit),
    onPinComplete: (String) -> Unit,
) {
    val viewModel = hiltViewModel<SessionProposalViewModel>()
    val sessionProposalUI = viewModel.getSessionProposalUI(version)
    if (sessionProposalUI == null) {
        Loading()
        return
    }
    val targetNetwork = viewModel.getTargetSwitchNetworks()

    Column(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .fillMaxWidth()
            .background(MixinAppTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_close_black_24dp),
            modifier = Modifier
                .size(52.dp, 52.dp)
                .align(alignment = Alignment.End)
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .clip(CircleShape)
                .clickable(onClick = {
                    onDismissRequest()
                }),
            contentDescription = null,
        )
        Box(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = R.string.Switch_Network),
            style = TextStyle(
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.W600,
            ),
        )
        Box(modifier = Modifier.height(8.dp))
        DAppInfo(
            info = "${sessionProposalUI.peer.name} (${sessionProposalUI.peer.uri})",
            icon = sessionProposalUI.peer.icon,
        )
        Box(modifier = Modifier.height(12.dp))
        Text(
            text = if (targetNetwork == null) {
                sessionProposalUI.chain.name
            } else {
                "${sessionProposalUI.chain.name} -> ${targetNetwork.name}"
            },
            fontSize = 18.sp,
            color = MixinAppTheme.colors.textPrimary,
        )
        Box(modifier = Modifier.height(12.dp))
        WCPinBoard(
            step = step,
            errorInfo = null,
            allowBiometric = true,
            onNegativeClick = { },
            onPositiveClick = { },
            onDoneClick = { onDismissRequest() },
            onBiometricClick = { onBiometricClick.invoke() },
            onPinComplete = { pin -> onPinComplete.invoke(pin) },
        )
    }
}
