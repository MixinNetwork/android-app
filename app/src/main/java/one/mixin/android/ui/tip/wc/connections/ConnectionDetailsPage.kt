package one.mixin.android.ui.tip.wc.connections

import GlideImage
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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme

@Composable
fun ConnectionDetailsPage(
    connectionId: Int?,
    pop: () -> Unit,
) {
    WCPageScaffold(
        title = "",
        verticalScrollable = false,
        pop = pop,
    ) {
        val viewModel = hiltViewModel<ConnectionsViewModel>()
        viewModel.currentConnectionId = connectionId
        val connectionUI by remember { viewModel.currentConnectionUI }
        if (connectionUI == null) {
            Loading()
        } else {
            Content(connectionUI = requireNotNull(connectionUI), onDisconnectClick = {
                viewModel.disconnect(WalletConnect.Version.V2, connectionUI!!.data)
                viewModel.refreshConnections()
                pop()
            })
        }
    }
}

@Composable
private fun Content(
    connectionUI: ConnectionUI,
    onDisconnectClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.height(28.dp))
        GlideImage(
            data = connectionUI.icon ?: "",
            modifier =
                Modifier
                    .size(90.dp)
                    .clip(CircleShape),
            placeHolderPainter = painterResource(id = R.drawable.ic_avatar_place_holder),
        )
        Box(modifier = Modifier.height(10.dp))
        Text(
            text = connectionUI.name,
            modifier = Modifier.padding(horizontal = 30.dp),
            fontSize = 18.sp,
            color = MixinAppTheme.colors.textPrimary,
        )
        Box(modifier = Modifier.height(4.dp))
        Text(
            text = connectionUI.uri,
            fontSize = 12.sp,
            color = MixinAppTheme.colors.textSubtitle,
        )
        Box(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = R.string.dapp_description),
            modifier = Modifier.padding(horizontal = 30.dp),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textPrimary,
        )
        Box(modifier = Modifier.height(24.dp))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MixinAppTheme.colors.backgroundWindow)
                    .clickable(onClick = onDisconnectClick),
        ) {
            Text(
                text = stringResource(id = R.string.disconnect),
                modifier =
                    Modifier
                        .padding(horizontal = 20.dp, vertical = 22.dp)
                        .align(Alignment.CenterStart),
                fontSize = 16.sp,
                color = MixinAppTheme.colors.textBlue,
            )
        }
    }
}

@Composable
fun Loading() {
    MixinAppTheme {
        Box(
            modifier =
                Modifier
                    .height(200.dp)
                    .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MixinAppTheme.colors.accent,
            )
        }
    }
}
