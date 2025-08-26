package one.mixin.android.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme

@Composable
fun InputAmountScreen(
    primaryAmount: String = "0 USD",
    minorAmount: String = "0 BTC",
    onNumberClick: (String) -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onSwitchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        // Primary amount display
        Text(
            text = primaryAmount,
            fontSize = 40.sp,
            fontWeight = FontWeight.Medium,
            color = MixinAppTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Minor amount with switch button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Text(
                text = minorAmount,
                fontSize = 16.sp,
                color = MixinAppTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
            
            IconButton(
                onClick = onSwitchClick,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_switch),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Number keyboard
        NumberKeyboard(
            onNumberClick = onNumberClick,
            onDeleteClick = onDeleteClick,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
fun NumberKeyboard(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        "1", "2", "3",
        "4", "5", "6", 
        "7", "8", "9",
        ".", "0", ""
    )
    
    Column(modifier = modifier) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            itemsIndexed(keys) { index, key ->
                when {
                    key.isEmpty() -> {
                        // Empty space
                        DeleteButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.padding(end = 24.dp)
                        )
                    }
                    key == "." -> {
                        KeyboardButton(
                            text = ".",
                            onClick = { onNumberClick(".") }
                        )
                    }
                    else -> {
                        KeyboardButton(
                            text = key,
                            onClick = { onNumberClick(key) }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun KeyboardButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MixinAppTheme.colors.background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = MixinAppTheme.colors.textPrimary
        )
    }
}

@Composable
fun DeleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MixinAppTheme.colors.background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_key_delete_white),
            contentDescription = "Delete",
        )
    }
}

@Composable
fun InputAmountScreenPreview() {
    MixinAppTheme {
        InputAmountScreen(
            primaryAmount = "1,234.56 USD",
            minorAmount = "0.0345 BTC"
        )
    }
}
