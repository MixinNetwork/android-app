package one.mixin.android.ui.wallet.components

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import one.mixin.android.R
import one.mixin.android.ui.home.web3.components.OutlinedTab
import one.mixin.android.vo.WalletCategory
import kotlin.math.abs

private const val ALL_CATEGORY_KEY = "all"

@Composable
fun WalletCategoryFilter(
    selectedCategory: String?,
    hasAll: Boolean = true,
    hasImported: Boolean = false,
    hasWatch: Boolean = false,
    hasSafe: Boolean = false,
    hasCreated: Boolean = true,
    showSafeBadge: Boolean = false,
    onCategorySelected: (String?) -> Unit
) {
    val tabs: List<WalletCategoryTab> = buildList {
        if (hasAll) {
            add(WalletCategoryTab(category = null, textResId = R.string.All, showBadge = false))
        }
        if (hasCreated) {
            add(WalletCategoryTab(category = WalletCategory.CLASSIC.value, textResId = R.string.Wallet_Created, showBadge = false))
        }
        if (hasImported) {
            add(WalletCategoryTab(category = "import", textResId = R.string.Wallet_Imported, showBadge = false))
        }
        if (hasWatch) {
            add(WalletCategoryTab(category = "watch", textResId = R.string.Wallet_Watching, showBadge = false))
        }
        if (hasSafe) {
            add(WalletCategoryTab(category = WalletCategory.MIXIN_SAFE.value, textResId = R.string.Wallet_Safe, showBadge = showSafeBadge))
        }
    }
    if (tabs.isEmpty()) return
    val selectedIndex: Int = tabs.indexOfFirst { tab: WalletCategoryTab -> tab.category == selectedCategory }.let { index: Int ->
        if (index >= 0) index else 0
    }
    WalletCategoryTabRow(
        tabs = tabs,
        selectedIndex = selectedIndex,
        onTabSelected = { index: Int ->
            tabs.getOrNull(index)?.let { selectedTab ->
                onCategorySelected(selectedTab.category)
            }
        }
    )
}

private data class WalletCategoryTab(
    val category: String?,
    val textResId: Int,
    val showBadge: Boolean
)

@Composable
private fun WalletCategoryTabRow(
    tabs: List<WalletCategoryTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    var viewportWidth by remember { mutableIntStateOf(0) }
    var lastSettledViewportWidth by remember { mutableIntStateOf(0) }
    var lastSettledTabKeys by remember { mutableStateOf<List<String>>(emptyList()) }
    var clickScrollToken by remember { mutableIntStateOf(0) }
    val tabKeys = remember(tabs) { tabs.map { it.category ?: ALL_CATEGORY_KEY } }

    LaunchedEffect(selectedIndex, tabKeys, viewportWidth, clickScrollToken) {
        if (viewportWidth <= 0 || tabs.isEmpty()) return@LaunchedEffect
        val shouldAnimate = lastSettledViewportWidth == viewportWidth
        val waitForLayout = tabKeys != lastSettledTabKeys
        if (listState.scrollToCenteredTab(selectedIndex, tabs.size, viewportWidth, shouldAnimate, waitForLayout)) {
            lastSettledViewportWidth = viewportWidth
            lastSettledTabKeys = tabKeys
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .onSizeChanged { size -> viewportWidth = size.width },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = tabs,
            key = { _: Int, tab: WalletCategoryTab -> tab.category ?: ALL_CATEGORY_KEY }
        ) { index: Int, tab: WalletCategoryTab ->
            OutlinedTab(
                text = stringResource(tab.textResId),
                selected = index == selectedIndex,
                showBadge = tab.showBadge,
                onClick = {
                    clickScrollToken++
                    onTabSelected(index)
                }
            )
        }
    }
}

private suspend fun LazyListState.scrollToCenteredTab(
    index: Int,
    totalCount: Int,
    expectedViewportWidth: Int,
    animate: Boolean,
    waitForLayout: Boolean,
): Boolean {
    if (index !in 0 until totalCount) return false
    if (
        waitForLayout ||
        layoutInfo.totalItemsCount != totalCount ||
        layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset != expectedViewportWidth
    ) {
        withFrameNanos { }
    }
    val initialLayoutInfo = layoutInfo
    val viewportWidth = initialLayoutInfo.viewportEndOffset - initialLayoutInfo.viewportStartOffset
    if (initialLayoutInfo.totalItemsCount != totalCount || viewportWidth <= 0) {
        return false
    }

    var itemInfo = initialLayoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (itemInfo == null) {
        val visibleItems = initialLayoutInfo.visibleItemsInfo
        val averageItemSize = if (visibleItems.isNotEmpty()) {
            visibleItems.sumOf { it.size } / visibleItems.size
        } else {
            0
        }
        val estimatedCenterOffset = -((viewportWidth - averageItemSize) / 2).coerceAtLeast(0)
        if (animate) {
            animateScrollToItem(index, estimatedCenterOffset)
        } else {
            scrollToItem(index, estimatedCenterOffset)
        }
        itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        if (itemInfo == null) {
            withFrameNanos { }
            itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        }
    }

    val targetItem = itemInfo ?: return false
    val currentLayoutInfo = layoutInfo
    val viewportCenter = (currentLayoutInfo.viewportStartOffset + currentLayoutInfo.viewportEndOffset) / 2
    val itemCenter = targetItem.offset + (targetItem.size / 2)
    val delta = itemCenter - viewportCenter
    val canScrollInDirection = (delta < 0 && canScrollBackward) || (delta > 0 && canScrollForward)
    if (abs(delta) > 1 && canScrollInDirection) {
        if (animate) {
            animateScrollBy(delta.toFloat())
        } else {
            scrollBy(delta.toFloat())
        }
    }
    return true
}
