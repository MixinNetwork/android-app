package one.mixin.android.ui.home

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.extension.isActivityEmbedded
import one.mixin.android.extension.isPhoneLandscape
import one.mixin.android.vo.ConversationCircleItem
import one.mixin.android.vo.getCircleColor

internal enum class HomeRailMode { None, Full, CompactLandscape }

internal fun Context.homeRailMode(): HomeRailMode = when {
    shouldShowHomeRail() -> HomeRailMode.Full
    isPhoneLandscape() -> HomeRailMode.CompactLandscape
    else -> HomeRailMode.None
}

// Embedded list panes can be narrower than 600dp.
internal fun Context.shouldShowHomeRail(): Boolean = isActivityEmbedded() || resources.configuration.smallestScreenWidthDp >= 600

private data class RailFilter(
    val filter: ConversationFilter,
    @param:DrawableRes
    val iconRes: Int,
    val labelRes: Int,
)

private val railFilters =
    listOf(
        RailFilter(ConversationFilter.All, R.drawable.ic_rail_chat, R.string.All_chats),
        RailFilter(ConversationFilter.Contacts, R.drawable.ic_rail_contacts, R.string.contacts_title),
        RailFilter(ConversationFilter.Groups, R.drawable.ic_rail_groups, R.string.Groups),
        RailFilter(ConversationFilter.Bots, R.drawable.ic_rail_bots, R.string.bots_title),
        RailFilter(ConversationFilter.Strangers, R.drawable.ic_rail_strangers, R.string.Strangers),
    )

@Composable
fun HomeRail(
    viewModel: HomeFilterViewModel,
    sectionId: Int,
    showWalletBadge: Boolean,
    showMoreBadge: Boolean,
    onFilterSelected: (ConversationFilter) -> Unit,
    onSectionSelected: (Int) -> Unit,
    compactLandscape: Boolean = false,
) {
    MixinAppTheme {
        val hairline = with(LocalDensity.current) { 1.toDp() }
        val border = MixinAppTheme.colors.borderPrimary
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MixinAppTheme.colors.background)
                .drawBehind {
                    val x = if (layoutDirection == LayoutDirection.Rtl) 0f else size.width - 1f
                    drawRect(border, topLeft = Offset(x, 0f), size = Size(1f, size.height))
                },
        ) {
            if (compactLandscape) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val availableHeight = maxHeight
                    Column(
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = availableHeight).padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    ) {
                        RailItem(
                            iconRes = if (sectionId == R.id.nav_chat) R.drawable.ic_tab_chat_selected else R.drawable.ic_tab_chat_unselected,
                            contentDescription = stringResource(R.string.Chat_bottom),
                            selected = sectionId == R.id.nav_chat,
                            showDot = rememberUnseen(viewModel, ConversationFilter.All),
                            onClick = { onSectionSelected(R.id.nav_chat) },
                        )
                        RailDestinations(sectionId, showWalletBadge, showMoreBadge, onSectionSelected, bottomPadding = 0.dp)
                    }
                }
                return@Box
            }
            val currentFilter by viewModel.filter.collectAsState()
            val circles by remember { viewModel.observeCircles() }.observeAsState(emptyList<ConversationCircleItem>())
            val chatsActive = sectionId == R.id.nav_chat
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 8.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    railFilters.forEach { spec ->
                        RailItem(
                            iconRes = spec.iconRes,
                            contentDescription = stringResource(spec.labelRes),
                            selected = chatsActive && currentFilter == spec.filter,
                            showDot = rememberUnseen(viewModel, spec.filter),
                            onClick = { onFilterSelected(spec.filter) },
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .width(32.dp)
                            .height(hairline)
                            .background(border),
                    )
                    circles.forEach { circle ->
                        key(circle.circleId) {
                            val filter = ConversationFilter.Circle(circle.circleId, circle.name)
                            RailItem(
                                iconRes = R.drawable.ic_rail_circle,
                                contentDescription = circle.name,
                                selected = chatsActive && currentFilter == filter,
                                showDot = rememberUnseen(viewModel, filter),
                                tint = Color(getCircleColor(circle.circleId)),
                                onClick = { onFilterSelected(filter) },
                            )
                        }
                    }
                }
                RailDestinations(
                    sectionId = sectionId,
                    showWalletBadge = showWalletBadge,
                    showMoreBadge = showMoreBadge,
                    onSectionSelected = onSectionSelected,
                )
            }
        }
    }
}

@Composable
private fun RailDestinations(
    sectionId: Int,
    showWalletBadge: Boolean,
    showMoreBadge: Boolean,
    onSectionSelected: (Int) -> Unit,
    bottomPadding: Dp = 8.dp,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RailItem(
            iconRes = if (sectionId == R.id.nav_market) R.drawable.ic_tab_market_selected else R.drawable.ic_tab_market_unselected,
            contentDescription = stringResource(R.string.Markets),
            selected = sectionId == R.id.nav_market,
            showDot = false,
            onClick = { onSectionSelected(R.id.nav_market) },
        )
        RailItem(
            iconRes = if (sectionId == R.id.nav_wallet) R.drawable.ic_tab_wallet_selected else R.drawable.ic_tab_wallet_unselected,
            contentDescription = stringResource(R.string.Wallets),
            selected = sectionId == R.id.nav_wallet,
            showDot = showWalletBadge,
            onClick = { onSectionSelected(R.id.nav_wallet) },
        )
        RailItem(
            iconRes = if (sectionId == R.id.nav_more) R.drawable.ic_tab_more_selected else R.drawable.ic_tab_more_unselected,
            contentDescription = stringResource(R.string.More),
            selected = sectionId == R.id.nav_more,
            showDot = showMoreBadge,
            onClick = { onSectionSelected(R.id.nav_more) },
        )
    }
}

@Composable
private fun RailItem(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    selected: Boolean,
    showDot: Boolean,
    onClick: () -> Unit,
    tint: Color = Color.Unspecified,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MixinAppTheme.colors.backgroundGrayLight else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = tint,
        )
        if (showDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(8.dp)
                    .background(MixinAppTheme.colors.badgeRed, CircleShape),
            )
        }
    }
}

@Composable
private fun rememberUnseen(viewModel: HomeFilterViewModel, filter: ConversationFilter): Boolean {
    val count by remember(filter) { viewModel.observeUnseenCount(filter) }.observeAsState(0)
    return count > 0
}
