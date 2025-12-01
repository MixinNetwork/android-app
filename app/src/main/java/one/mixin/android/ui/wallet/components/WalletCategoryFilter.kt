package one.mixin.android.ui.wallet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.vo.WalletCategory

@Composable
fun WalletCategoryFilter(
    selectedCategory: String?,
    hasImported: Boolean = false,
    hasWatch: Boolean = false,
    hasSafe: Boolean = false,
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
        CategoryChip(
            text = stringResource(R.string.All),
            isSelected = selectedCategory == null,
            onClick = { onCategorySelected(null) }
        )
        
        // Safe - show if hasSafe is true OR if hasImported/hasWatch are provided (full mode)
        if (hasSafe || hasImported || hasWatch) {
            CategoryChip(
                text = stringResource(R.string.Wallet_Safe),
                isSelected = selectedCategory == WalletCategory.MIXIN_SAFE.value,
                onClick = { onCategorySelected(WalletCategory.MIXIN_SAFE.value) }
            )
        }

        // Created (Classic) - only show in full mode
        if (hasImported || hasWatch) {
            CategoryChip(
                text = stringResource(R.string.Wallet_Created),
                isSelected = selectedCategory == WalletCategory.CLASSIC.value,
                onClick = { onCategorySelected(WalletCategory.CLASSIC.value) }
            )
        }
        
        // Import
        if (hasImported) {
            CategoryChip(
                text = stringResource(R.string.Wallet_Imported),
                isSelected = selectedCategory == "import",
                onClick = { onCategorySelected("import") }
            )
        }
        
        // Watching
        if (hasWatch) {
            CategoryChip(
                text = stringResource(R.string.Wallet_Watching),
                isSelected = selectedCategory == "watch",
                onClick = { onCategorySelected("watch") }
            )
        }
    }
}

@Composable
fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (isSelected) MixinAppTheme.colors.accent else MixinAppTheme.colors.borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .background(
                color = if (isSelected) MixinAppTheme.colors.backgroundGrayLight else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) MixinAppTheme.colors.accent else MixinAppTheme.colors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
