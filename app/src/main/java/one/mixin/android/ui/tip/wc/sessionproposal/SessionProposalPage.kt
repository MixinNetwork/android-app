package one.mixin.android.ui.tip.wc.sessionproposal

import GlideImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.ui.tip.wc.WalletConnectBottomSheetDialogFragment
import one.mixin.android.ui.tip.wc.connections.Loading

@Composable
fun SessionProposalPage(
    version: WalletConnect.Version,
    step: WalletConnectBottomSheetDialogFragment.Step,
    errorInfo: String?,
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
                .size(40.dp, 40.dp)
                .align(alignment = Alignment.End)
                .padding(horizontal = 8.dp)
                .clip(CircleShape)
                .clickable(onClick = {
                    viewModel.rejectSession(version)
                    onDismissRequest.invoke()
                }),
            contentDescription = null,
        )
        Box(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = R.string.Connect_Wallet),
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
        Box(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(8.dp))
                .background(MixinAppTheme.colors.backgroundWindow),
        ) {
            Scope(
                name = stringResource(id = R.string.Read_your_public_address),
                desc = stringResource(id = R.string.Allow_app_access_wallet_balance_activity),
            )
            Scope(
                name = stringResource(id = R.string.Request_permission),
                desc = stringResource(id = R.string.Allow_dapp_ask_permission),
            )
        }
        Network(name = sessionProposalUI.chain.name)
        WCPinBoard(
            step = step,
            errorInfo = errorInfo,
            allowBiometric = true,
            onNegativeClick = { },
            onPositiveClick = { },
            onDoneClick = { onDismissRequest() },
            onBiometricClick = { onBiometricClick.invoke() },
            onPinComplete = { pin -> onPinComplete.invoke(pin) },
        )
    }
}

@Composable
fun DAppInfo(
    modifier: Modifier = Modifier,
    info: String,
    icon: String,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        GlideImage(
            data = icon,
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape),
            placeHolderPainter = painterResource(id = R.drawable.ic_avatar_place_holder),
        )
        Box(modifier = Modifier.width(4.dp))
        Text(
            text = info,
            color = MixinAppTheme.colors.textSubtitle,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Scope(
    name: String,
    desc: String,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth(),

    ) {
        Image(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .padding(end = 8.dp),
            painter = painterResource(id = R.drawable.ic_selected_disable),
            contentDescription = null,
        )
        Column(
            modifier = Modifier.align(alignment = Alignment.Top),
        ) {
            Text(
                name,
                fontSize = 16.sp,
                color = MixinAppTheme.colors.textPrimary,
            )
            Text(
                desc,
                fontSize = 14.sp,
                color = MixinAppTheme.colors.textSubtitle,
            )
        }
    }
}

@Composable
private fun Network(
    name: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(id = R.string.Network),
            color = MixinAppTheme.colors.textSubtitle,
            fontSize = 14.sp,
        )
        Text(
            text = name,
            color = MixinAppTheme.colors.textSubtitle,
            fontSize = 14.sp,
        )
    }
}
