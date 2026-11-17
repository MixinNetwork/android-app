package one.mixin.android.ui.home.web3.trade.perps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsPosition
import one.mixin.android.api.response.perps.PerpsPositionItem
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.vo.safe.TokenItem

@Preview(name = "中文", group = "Perps confirmations", widthDp = 393, locale = "zh")
@Preview(name = "窄屏", group = "Perps confirmations", widthDp = 320, locale = "zh", fontScale = 1.15f)
@Preview(name = "English", group = "Perps confirmations", widthDp = 393, locale = "en")
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class PerpsSheetPreview

@Composable
internal fun PerpsPreviewContent(title: Int, content: @Composable () -> Unit) {
    PerpsConfirmationTheme {
        Column(Modifier.fillMaxWidth().background(MixinAppTheme.colors.background).padding(vertical = 20.dp)) {
            Text(stringResource(title), Modifier.padding(horizontal = 20.dp), color = MixinAppTheme.colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.W600)
            Spacer(Modifier.height(20.dp))
            content()
        }
    }
}

internal object PerpsPreviewData {
    val position = PerpsPositionItem(
        positionId = "preview-position",
        marketId = "preview-btc",
        side = "long",
        quantity = "0.01234567",
        entryPrice = "65000",
        leverage = 10,
        margin = "80.126855",
        unrealizedPnl = "-0.12",
        estimatedCloseFee = "0.08",
        liquidationPrice = "58500",
        markPrice = "64990.28",
        tokenSymbol = "BTC",
        displaySymbol = "BTC-USDC",
        state = PerpsPosition.STATE_OPEN,
    )
    val token = TokenItem(
        assetId = "preview-usdc", symbol = "USDC", name = "USD Coin", iconUrl = "",
        balance = "1000", priceBtc = "0", priceUsd = "1", chainId = "preview-chain",
        changeUsd = "0", changeBtc = "0", hidden = false, confirmations = 1,
        chainIconUrl = null, chainSymbol = "ETH", chainName = "Ethereum", assetKey = null,
        dust = null, withdrawalMemoPossibility = null, collectionHash = null, level = null, precision = 6,
    )
}
