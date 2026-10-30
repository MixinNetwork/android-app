package one.mixin.android.ui.wallet.network

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
internal fun rememberNetworkIconUrls(
    viewModel: NetworkIconViewModel,
    networks: List<NetworkOption>,
): Map<String, String> {
    val chainIds = remember(networks) { networks.map { it.chainId }.toSet() }
    var iconUrls by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(chainIds) {
        iconUrls = viewModel.iconUrls(chainIds)
    }
    return iconUrls
}

internal fun filterNetworks(networks: List<NetworkOption>, query: String): List<NetworkOption> {
    val keyword = query.trim()
    if (keyword.isEmpty()) return networks
    return networks.filter { it.name.contains(keyword, ignoreCase = true) }
}

@Composable
internal fun NetworkIcon(
    iconUrl: String?,
    size: Dp,
) {
    CoilImage(
        model = iconUrl,
        placeholder = R.drawable.ic_avatar_place_holder,
        modifier = Modifier
            .size(size)
            .clip(CircleShape),
    )
}

@Composable
internal fun NetworkOptionRow(
    name: String,
    iconUrl: String?,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NetworkIcon(iconUrl = iconUrl, size = 32.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            color = MixinAppTheme.colors.textPrimary,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(12.dp))
        trailing()
    }
}
