package one.mixin.android.ui.wallet.alert

import PageScaffold
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.compose.CoilImage
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.priceFormat
import one.mixin.android.ui.wallet.alert.vo.Alert
import one.mixin.android.ui.wallet.alert.vo.AlertGroup
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal

@Composable
fun AlertPage(assets: List<TokenItem>?, openFilter: () -> Unit, pop: () -> Unit, to: () -> Unit) {
    val viewModel = hiltViewModel<AlertViewModel>()
    val alertGroups by viewModel.alertGroups().collectAsState(initial = emptyList())

    PageScaffold(
        title = stringResource(id = R.string.Alert),
        verticalScrollable = false, // Disable vertical scrolling here
        pop = pop,
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxSize()
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssetFilter(assets, openFilter)
                    TextButton(
                        onClick = to,
                        modifier = Modifier.wrapContentSize(),
                    ) {
                        Image(
                            modifier = Modifier.size(18.dp),
                            painter = painterResource(R.drawable.ic_alert_add),
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.Add_Alert), color = Color(0xFF3D75E3)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (alertGroups.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(painter = painterResource(R.drawable.ic_empty_file), contentDescription = null)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(text = stringResource(R.string.NO_ALERTS), color = MixinAppTheme.colors.textRemarks)
                    }
                }
            } else {
                items(alertGroups.size) { index ->
                    if (index != 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    AlertGroupItem(alertGroups[index], index == 0)
                }
            }
        }
    }
}

@Preview
@Composable
fun AlertPagePreview() {
}

@Composable
fun AssetFilter(assets: List<TokenItem>?, open: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .background(MixinAppTheme.colors.background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                open.invoke()
            }
            .border(1.dp, shape = RoundedCornerShape(24.dp), color = MixinAppTheme.colors.backgroundWindow)
    ) {
        if (assets.isNullOrEmpty()) {
            Row(
                modifier = Modifier.padding(vertical = 9.dp, horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.Assets), fontSize = 14.sp, color = MixinAppTheme.colors.textPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    modifier = Modifier.wrapContentSize(),
                    painter = painterResource(id = R.drawable.ic_sort_arrow_down),
                    contentDescription = null,
                    tint = Color.Unspecified,
                )
            }
        } else if (assets.size == 1) {
            Row(
                modifier = Modifier.padding(vertical = 9.dp, horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoilImage(
                    assets.first().iconUrl,
                    modifier =
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape),
                    placeholder = R.drawable.ic_avatar_place_holder,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(assets.first().symbol, fontSize = 14.sp, color = MixinAppTheme.colors.textPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    modifier = Modifier.wrapContentSize(),
                    painter = painterResource(id = R.drawable.ic_sort_arrow_down),
                    contentDescription = null,
                    tint = Color.Unspecified,
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(vertical = 9.dp, horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OverlappingLayout {
                    assets.forEachIndexed { _, item ->
                        CoilImage(
                            item.iconUrl,
                            modifier =
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape),
                            placeholder = R.drawable.ic_avatar_place_holder,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.number_of_assets, assets.size), fontSize = 14.sp, color = MixinAppTheme.colors.textPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    modifier = Modifier.wrapContentSize(),
                    painter = painterResource(id = R.drawable.ic_sort_arrow_down),
                    contentDescription = null,
                    tint = Color.Unspecified,
                )
            }
        }
    }
}

@Composable
fun OverlappingLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }
        val itemWidth = placeables.firstOrNull()?.width ?: 0
        val overlapWidth = itemWidth / 2

        val totalWidth = itemWidth + (placeables.size - 1) * overlapWidth

        val totalHeight = placeables.maxOfOrNull { it.height } ?: 0

        layout(totalWidth, totalHeight) {
            var xPosition = 0

            placeables.forEach { placeable ->
                placeable.placeRelative(x = xPosition, y = 0)
                xPosition += overlapWidth
            }
        }
    }
}

@Composable
fun AlertGroupItem(alertGroup: AlertGroup, initiallyExpanded: Boolean) {
    var expand by remember { mutableStateOf(initiallyExpanded) }
    val viewModel = hiltViewModel<AlertViewModel>()
    val alerts by viewModel.alertsByAssetId(alertGroup.assetId).collectAsState(initial = emptyList())

    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MixinAppTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expand = !expand },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    CoilImage(
                        alertGroup.iconUrl,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape),
                        placeholder = R.drawable.ic_avatar_place_holder,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(alertGroup.name, fontSize = 16.sp, color = MixinAppTheme.colors.textPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.Current_price, "${BigDecimal(alertGroup.priceUsd).priceFormat()}${Fiats.getAccountCurrencyAppearance()}"),
                            fontSize = 13.sp,
                            color = MixinAppTheme.colors.textAssist
                        )
                    }
                }
                Icon(
                    modifier = Modifier.wrapContentSize(),
                    painter = painterResource(id = R.drawable.ic_sort_arrow_down),
                    contentDescription = null,
                    tint = Color.Unspecified,
                )
            }

            AnimatedVisibility(
                modifier = Modifier
                    .fillMaxWidth(), visible = expand, enter = fadeIn(), exit = fadeOut()
            ) {
                alerts.forEachIndexed { index, alert ->
                    if (index != 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    AlertItem(alert)
                }
            }
        }
    }
}

@Composable
fun AlertItem(alert: Alert) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Icon(
            modifier = Modifier.wrapContentSize(),
            painter = painterResource(id = alert.type.getIconResId()),
            contentDescription = null,
            tint = Color.Unspecified,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(BigDecimal(alert.value).priceFormat(), fontSize = 14.sp, color = MixinAppTheme.colors.textPrimary)
            Text(stringResource(alert.frequency.getStringResId()), fontSize = 12.sp, color = MixinAppTheme.colors.textAssist)
        }
        Icon(
            modifier = Modifier.wrapContentSize(),
            painter = painterResource(id = R.drawable.ic_more_horiz_black_24dp),
            contentDescription = null,
            tint = Color.Unspecified,
        )
    }
}