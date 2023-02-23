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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import one.mixin.android.R
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.ui.common.compose.SearchTextField
import one.mixin.android.ui.setting.ui.compose.HighlightText
import one.mixin.android.ui.setting.ui.compose.SettingPageScaffold
import one.mixin.android.ui.setting.ui.theme.MixinAppTheme
import one.mixin.android.ui.tip.wc.LocalWCNav
import one.mixin.android.ui.tip.wc.WCDestination

@Composable
fun ConnectionsPage() {
    SettingPageScaffold(
        title = stringResource(id = R.string.Connected_dapps),
        verticalScrollable = true,
    ) {
        val viewModel = hiltViewModel<ConnectionsViewModel>()

        val text = rememberSaveable {
            mutableStateOf("")
        }
        SearchTextField(text, stringResource(id = R.string.connected_dapp_search_hint))

        viewModel.refreshConnections()
        val connections by viewModel.connections.collectAsState(initial = emptyList())

        if (connections.isEmpty()) {
            EmptyLayout()
        } else {
            ConnectionList(viewModel, connections, keyword = text.value.trim())
        }
    }
}

@Composable
private fun ConnectionList(
    viewModel: ConnectionsViewModel,
    data: List<ConnectionUI>,
    keyword: String,
) {
    val filteredData = remember(data, keyword) {
        if (keyword.isEmpty()) {
            data
        } else {
            data.filter {
                it.name.containsIgnoreCase(keyword)
            }
        }
    }
    LazyColumn {
        items(filteredData, key = {
            listOf(it.uri, keyword)
        }) { item ->
            val navController = LocalWCNav.current
            ConnectionItem(item, keyword) {
                viewModel.currentConnectionId = item.index
                navController.navTo(WCDestination.ConnectionDetails)
            }
        }
    }
}

@Composable
private fun ConnectionItem(
    connectionUI: ConnectionUI,
    highlight: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(MixinAppTheme.colors.background)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(20.dp))
        GlideImage(
            data = connectionUI.icon ?: "",
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape),
            placeHolderPainter = painterResource(id = R.drawable.ic_avatar_place_holder),
        )
        Box(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start,
        ) {
            HighlightText(
                text = connectionUI.name,
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = MixinAppTheme.colors.textPrimary,
                ),
                overflow = TextOverflow.Ellipsis,
                target = highlight, 
            )
            Box(modifier = Modifier.width(8.dp))
            HighlightText(
                text = connectionUI.uri,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = MixinAppTheme.colors.textSubtitle,
                ),
                overflow = TextOverflow.Ellipsis,
                target = highlight,
            )
        }
        Box(modifier = Modifier.width(20.dp))
    }
}

@Composable
private fun EmptyLayout() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = (-42).dp),
        ) {
            Image(
                modifier = Modifier.size(42.dp),
                painter = painterResource(id = R.drawable.ic_authentication),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MixinAppTheme.colors.textSubtitle),
            )
            Box(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.NO_CONNECTED_DAPPS),
                fontSize = 16.sp,
                color = MixinAppTheme.colors.textSubtitle,
            )
        }
    }
}