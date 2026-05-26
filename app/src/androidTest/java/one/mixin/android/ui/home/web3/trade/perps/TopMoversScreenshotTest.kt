package one.mixin.android.ui.home.web3.trade.perps

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import one.mixin.android.HiltTestActivity
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.compose.theme.MixinAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class TopMoversScreenshotTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Test
    fun captureTopMoversCard() {
        hiltRule.inject()

        composeTestRule.setContent {
            MixinAppTheme(darkTheme = false) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                ) {
                    TopMoversCard(
                        markets = topMoverMarkets,
                        quoteColorReversed = false,
                        onViewAllClick = {},
                        onMarketItemClick = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("BTC").assertExists()

        val activity = composeTestRule.activity
        val root = activity.window.decorView.rootView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        root.draw(canvas)
        
        val outputDir = File("/sdcard/Android/media/one.mixin.messenger/additional_test_output")
        if (!outputDir.exists()) outputDir.mkdirs()
        val screenshot = File(outputDir, "top_movers_card.png")
        FileOutputStream(screenshot).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    private val topMoverMarkets = listOf(
        topMoverMarket("bitcoin", "BTC", "0.1284", 125),
        topMoverMarket("ethereum", "ETH", "0.0931", 100),
        topMoverMarket("solana", "SOL", "0.0715", 75),
        topMoverMarket("dogecoin", "DOGE", "0.0548", 50),
        topMoverMarket("sui", "SUI", "-0.0324", 50),
        topMoverMarket("hyperliquid", "HYPE", "-0.0417", 25),
        topMoverMarket("ripple", "XRP", "-0.0589", 50),
        topMoverMarket("toncoin", "TON", "-0.0862", 25),
    )

    private fun topMoverMarket(
        id: String,
        symbol: String,
        change: String,
        leverage: Int,
    ) = PerpsMarket(
        marketId = id,
        displaySymbol = "$symbol-PERP",
        tokenSymbol = symbol,
        quoteSymbol = "USDT",
        markPrice = "100.00",
        leverage = leverage,
        iconUrl = "",
        fundingRate = "0.0001",
        minAmount = "1",
        maxAmount = "100000",
        last = "100.00",
        volume = "1000000",
        high = "120.00",
        low = "90.00",
        open = "95.00",
        change = change,
        bidPrice = "99.90",
        askPrice = "100.10",
        createdAt = "2026-05-26T00:00:00Z",
        updatedAt = "2026-05-26T00:00:00Z",
    )
}
