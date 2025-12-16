package one.mixin.android.ui.wallet.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import one.mixin.android.R
import one.mixin.android.ui.home.web3.components.OutlinedTab
import one.mixin.android.vo.WalletCategory

@Composable
fun WalletCategoryFilter(
    selectedCategory: String?,
    hasImported: Boolean = false,
    hasWatch: Boolean = false,
    hasSafe: Boolean = false,
    hasCreated: Boolean = true,
    showSafeBadge: Boolean = false,
    onCategorySelected: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(vertical = 4.dp)
        ,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All
        OutlinedTab(
            text = stringResource(R.string.All),
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
        )
        
        if (hasSafe) {
            OutlinedTab(
                text = stringResource(R.string.Wallet_Safe),
                selected = selectedCategory == WalletCategory.MIXIN_SAFE.value,
                showBadge = showSafeBadge,
                onClick = { onCategorySelected(WalletCategory.MIXIN_SAFE.value) },
            )
        }

        if (hasCreated) {
            OutlinedTab(
                text = stringResource(R.string.Wallet_Created),
                selected = selectedCategory == WalletCategory.CLASSIC.value,
                onClick = { onCategorySelected(WalletCategory.CLASSIC.value) },
            )
        }

        // Import
        if (hasImported) {
            OutlinedTab(
                text = stringResource(R.string.Wallet_Imported),
                selected = selectedCategory == "import",
                onClick = { onCategorySelected("import") },
            )
        }
        
        // Watching
        if (hasWatch) {
            OutlinedTab(
                text = stringResource(R.string.Wallet_Watching),
                selected = selectedCategory == "watch",
                onClick = { onCategorySelected("watch") },
            )
        }
    }
}
