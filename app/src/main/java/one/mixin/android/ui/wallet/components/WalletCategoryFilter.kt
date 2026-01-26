package one.mixin.android.ui.wallet.components

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import one.mixin.android.R
import one.mixin.android.ui.home.web3.components.OutlinedTab
import one.mixin.android.vo.WalletCategory

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
    val tabs: MutableList<WalletCategoryTab> = mutableListOf()
    if (hasAll) {
        tabs.add(WalletCategoryTab(category = null, textResId = R.string.All, showBadge = false))
    }
    if (hasSafe) {
        tabs.add(WalletCategoryTab(category = WalletCategory.MIXIN_SAFE.value, textResId = R.string.Wallet_Safe, showBadge = showSafeBadge))
    }
    if (hasCreated) {
        tabs.add(WalletCategoryTab(category = WalletCategory.CLASSIC.value, textResId = R.string.Wallet_Created, showBadge = false))
    }
    if (hasImported) {
        tabs.add(WalletCategoryTab(category = "import", textResId = R.string.Wallet_Imported, showBadge = false))
    }
    if (hasWatch) {
        tabs.add(WalletCategoryTab(category = "watch", textResId = R.string.Wallet_Watching, showBadge = false))
    }
    val selectedIndex: Int = tabs.indexOfFirst { tab: WalletCategoryTab -> tab.category == selectedCategory }.let { index: Int ->
        if (index >= 0) index else 0
    }
    WalletCategoryTabRow(
        tabs = tabs,
        selectedIndex = selectedIndex,
        onTabSelected = { index: Int ->
            val selectedTab: WalletCategoryTab = tabs[index]
            onCategorySelected(selectedTab.category)
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
    val listState: LazyListState = rememberLazyListState()
    LaunchedEffect(selectedIndex, tabs.size) {
        if (tabs.isEmpty()) return@LaunchedEffect
        listState.animateScrollToItem(index = selectedIndex)
        val delta: Float? = withFrameNanos { _: Long ->
            val itemInfo: LazyListItemInfo? = listState.layoutInfo.visibleItemsInfo
                .firstOrNull { info: LazyListItemInfo -> info.index == selectedIndex }
            if (itemInfo == null) return@withFrameNanos null
            val viewportStartOffset: Int = listState.layoutInfo.viewportStartOffset
            val viewportEndOffset: Int = listState.layoutInfo.viewportEndOffset
            val viewportWidth: Int = viewportEndOffset - viewportStartOffset
            val viewportCenter: Int = viewportStartOffset + (viewportWidth / 2)
            val itemCenter: Int = itemInfo.offset + (itemInfo.size / 2)
            (itemCenter - viewportCenter).toFloat()
        }
        if (delta != null && delta != 0f) {
            listState.animateScrollBy(delta)
        }
    }
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = tabs,
            key = { index: Int, tab: WalletCategoryTab -> "${tab.category ?: "all"}-$index" }
        ) { index: Int, tab: WalletCategoryTab ->
            OutlinedTab(
                text = stringResource(tab.textResId),
                selected = index == selectedIndex,
                showBadge = tab.showBadge,
                onClick = { onTabSelected(index) },
            )
        }
    }
}
