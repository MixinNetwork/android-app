package one.mixin.android.ui.wallet.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.ui.wallet.alert.components.cardBackground

enum class WalletDestination {
    Privacy,
    Classic
}

@Composable
fun WalletScreen(
    onWalletCardClick: (destination: WalletDestination, walletId: String?) -> Unit
) {
    MixinAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.Wallet),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MixinAppTheme.colors.textPrimary,
                modifier = Modifier
                    .height(56.dp)
                    .wrapContentHeight(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.height(10.dp))
            TotalAssetsCard()
            Spacer(modifier = Modifier.height(20.dp))
            WalletCard(
                title = "Privacy Wallet",
                onClick = { onWalletCardClick.invoke(WalletDestination.Privacy, "privacy_001") }
            )
            Spacer(modifier = Modifier.height(10.dp))
            WalletCard(
                title = "Classic Wallet",
                onClick = { onWalletCardClick.invoke(WalletDestination.Classic, "classic_001") }
            )
        }
    }
}

data class AssetDistribution(
    val currency: String,
    val percentage: Float, // 0.0 - 1.0
)

// Todo remove
val sampleDistribution = listOf<AssetDistribution>(
    AssetDistribution("BTC", 0.65f),
    AssetDistribution("ETH", 0.25f),
    AssetDistribution("XIN", 0.1f)
)

@Composable
fun TotalAssetsCard() {
    val distributions = remember { sampleDistribution }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text("Total assets", color = MixinAppTheme .colors.textPrimary )
        Spacer(modifier = Modifier.width(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "123,456.78",
                color = MixinAppTheme.colors.textPrimary,
                fontSize = 40.sp,
                fontWeight = FontWeight.W400,
                fontFamily = FontFamily(Font(R.font.mixin_font))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("USD", color = MixinAppTheme.colors.textAssist, fontSize = 12.sp)
        }
        Spacer(Modifier.height(20.dp))
        MultiColorProgressBar(distributions)
        Spacer(Modifier.height(12.dp))
        Distribution(distributions)
    }
}



@Composable
fun WalletCard(
    title: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(MixinAppTheme.colors.background, MixinAppTheme.colors.borderColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current
            ) { onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("1000 USD", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            // Add a composable for the percentage distribution here
        }
    }
}