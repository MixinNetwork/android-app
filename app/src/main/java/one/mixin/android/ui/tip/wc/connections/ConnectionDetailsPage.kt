package one.mixin.android.ui.tip.wc.connections

import GlideImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import one.mixin.android.tip.wc.Chain
import one.mixin.android.ui.setting.ui.compose.MixinBottomSheetDialog
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.ui.tip.wc.LocalWCNav

@Composable
fun ConnectionDetailsPage(connectionId: Int?) {
    WCPageScaffold(
        title = "",
        verticalScrollable = false,
    ) {
        val viewModel = hiltViewModel<ConnectionsViewModel>()
        viewModel.currentConnectionId = connectionId
        val connectionUI by remember { viewModel.currentConnectionUI }
        if (connectionUI == null) {
            Loading()
        } else {
            val navController = LocalWCNav.current
            var openBottomSheet by rememberSaveable { mutableStateOf(false) }
            Content(connectionUI = requireNotNull(connectionUI), {
                openBottomSheet = true
            }) {
                viewModel.disconnect(connectionUI!!.data)
                viewModel.refreshConnections()
                navController.pop()
            }
            if (openBottomSheet) {
                NetworkBottomSheet(
                    onDismissRequest = { openBottomSheet = false },
                    onItemClick = { chain ->
                        openBottomSheet = false
                        viewModel.changeNetworkV1(chain)
                    },
                )
            }
        }
    }
}

@Composable
private fun Content(
    connectionUI: ConnectionUI,
    onNetworkClick: () -> Unit,
    onDisconnectClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.height(28.dp))
        GlideImage(
            data = connectionUI.icon ?: "",
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape),
            placeHolderPainter = painterResource(id = R.drawable.ic_avatar_place_holder),
        )
        Box(modifier = Modifier.height(10.dp))
        Text(
            text = connectionUI.name,
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
            text = stringResource(id = R.string.connected_desc),
            modifier = Modifier.padding(horizontal = 30.dp),
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = MixinAppTheme.colors.textPrimary,
        )
        Box(modifier = Modifier.height(24.dp))
        if (connectionUI.chain != null) {
            Network(name = connectionUI.chain.name, onClick = onNetworkClick)
            Box(modifier = Modifier.height(10.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(MixinAppTheme.colors.backgroundWindow)
                .clickable(onClick = onDisconnectClick),
        ) {
            Text(
                text = stringResource(id = R.string.Disconnect),
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 22.dp)
                    .align(Alignment.CenterStart),
                fontSize = 16.sp,
                color = MixinAppTheme.colors.textBlue,
            )
        }
    }
}

@Composable
private fun Network(
    name: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(MixinAppTheme.colors.backgroundWindow)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.Network),
            fontSize = 16.sp,
            color = MixinAppTheme.colors.textPrimary,
        )
        Box(modifier = Modifier.weight(1f))
        Text(
            text = name,
            fontSize = 15.sp,
            color = MixinAppTheme.colors.textSubtitle,
        )
        Box(modifier = Modifier.width(12.dp))
        Image(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
        )
    }
}

@Composable
private fun NetworkBottomSheet(
    chains: List<Chain> = arrayListOf(Chain.Ethereum, Chain.Polygon, Chain.BinanceSmartChain),
    onDismissRequest: () -> Unit,
    onItemClick: (Chain) -> Unit,
) {
    MixinBottomSheetDialog(onDismissRequest = onDismissRequest) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(MixinAppTheme.colors.background),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(chains) { chain ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { onItemClick(chain) }
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        modifier = Modifier.align(Alignment.CenterStart),
                        text = chain.name,
                        fontSize = 14.sp,
                        color = MixinAppTheme.colors.textPrimary,
                    )
                }
            }
        }
    }
}

@Composable
fun Loading() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MixinAppTheme.colors.accent,
        )
    }
}
