package one.mixin.android.ui.wallet.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
    if (hasCreated) {
        tabs.add(WalletCategoryTab(category = WalletCategory.CLASSIC.value, textResId = R.string.Wallet_Created, showBadge = false))
    }
    if (hasImported) {
        tabs.add(WalletCategoryTab(category = "import", textResId = R.string.Wallet_Imported, showBadge = false))
    }
    if (hasWatch) {
        tabs.add(WalletCategoryTab(category = "watch", textResId = R.string.Wallet_Watching, showBadge = false))
    }
    if (hasSafe) {
        tabs.add(WalletCategoryTab(category = WalletCategory.MIXIN_SAFE.value, textResId = R.string.Wallet_Safe, showBadge = showSafeBadge))
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
    val scrollState = rememberScrollState()
    val tabWidths = remember { mutableStateMapOf<Int, Int>() }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val viewportWidthPx = with(density) { maxWidth.roundToPx() }
        val horizontalPaddingPx = with(density) { 16.dp.roundToPx() }
        val tabSpacingPx = with(density) { 8.dp.roundToPx() }
        val tabWidthsSnapshot = tabWidths.toMap()

        LaunchedEffect(selectedIndex, viewportWidthPx, scrollState.maxValue, tabWidthsSnapshot) {
            val selectedTabWidthPx = tabWidthsSnapshot[selectedIndex] ?: return@LaunchedEffect
            if ((0 until selectedIndex).any { index -> tabWidthsSnapshot[index] == null }) {
                return@LaunchedEffect
            }

            val widthBeforeSelectedPx = (0 until selectedIndex).sumOf { index ->
                tabWidthsSnapshot.getValue(index)
            }
            val selectedTabCenterPx = horizontalPaddingPx +
                widthBeforeSelectedPx +
                (selectedIndex * tabSpacingPx) +
                (selectedTabWidthPx / 2)
            val targetScrollPx = (selectedTabCenterPx - (viewportWidthPx / 2))
                .coerceIn(0, scrollState.maxValue)

            scrollState.animateScrollTo(targetScrollPx)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            tabs.forEachIndexed { index: Int, tab: WalletCategoryTab ->
                Box(
                    modifier = Modifier
                        .padding(end = if (index == tabs.lastIndex) 0.dp else 8.dp)
                        .onSizeChanged { size ->
                            tabWidths[index] = size.width
                        }
                ) {
                    OutlinedTab(
                        text = stringResource(tab.textResId),
                        selected = index == selectedIndex,
                        showBadge = tab.showBadge,
                        onClick = { onTabSelected(index) },
                    )
                }
            }
        }
    }
}
