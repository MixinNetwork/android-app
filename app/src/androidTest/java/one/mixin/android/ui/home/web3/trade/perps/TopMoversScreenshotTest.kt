package one.mixin.android.ui.home.web3.trade.perps

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class TopMoversScreenshotTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    @Test
    fun captureTopMoversCard_Light() {
        captureScreenshot(darkTheme = false, fileName = "top_movers_card_light.png")
    }

    @Test
    fun captureTopMoversCard_Dark() {
        captureScreenshot(darkTheme = true, fileName = "top_movers_card_dark.png")
    }

    private fun captureScreenshot(darkTheme: Boolean, fileName: String) {
        hiltRule.inject()

        composeTestRule.setContent {
            MixinAppTheme(darkTheme = darkTheme) {
                Surface(
                    color = MixinAppTheme.colors.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
        }

        composeTestRule.onNodeWithText("HYPE").assertExists()

        Thread.sleep(5000)

        val activity = composeTestRule.activity
        val window = activity.window
        val view = window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val latch = CountDownLatch(1)
        
        PixelCopy.request(window, bitmap, { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {
                latch.countDown()
            }
        }, Handler(Looper.getMainLooper()))
        
        latch.await(5, TimeUnit.SECONDS)
        
        val outputDir = File("/sdcard/Android/media/one.mixin.messenger/additional_test_output")
        if (!outputDir.exists()) outputDir.mkdirs()
        val screenshot = File(outputDir, fileName)
        FileOutputStream(screenshot).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    private val topMoverMarkets = listOf(
        topMoverMarket("04315ccb-211c-3a12-b28f-60fec2ea69e8", "HYPE", "0.0268", 100, "https://coin-images.coingecko.com/coins/images/50882/large/hyperliquid.jpg?1729431300"),
        topMoverMarket("411aae6f-2596-3668-9fca-85f1c4dcd3c6", "TON", "0.0245", 100, "https://images.mixin.one/Qh7MjeINQ6ad68E0FI4iS7bbLGEuF7CZJlTkW1kSAiq8EaFngIZ1tDG0CRHz_hjz8gsiTmHKcdu_0UE1ugmUiHzNwJRm9fjoqJcb=s128"),
        topMoverMarket("2c03fc3c-f7c9-39bd-8cdb-ba8a52476dc1", "ALGO", "0.0240", 100, "https://images.mixin.one/-oE4Jsi3aMIkxUPUsvDozyL8D0ccmPkggIdIDu1z8THDQyJcCIsbNwC4amFBiRlkQiLNNjiuMBsNw8sAnehTuI0=s128"),
        topMoverMarket("c0349d7b-1b40-3fb7-804a-475abf4aadb7", "BERA", "0.0195", 100, "https://coin-images.coingecko.com/coins/images/25235/large/BERA.png?1738822008"),
        topMoverMarket("9aa033e3-5ee4-320c-aa7f-b55b6ccd3a4b", "UNI", "-0.0229", 100, "https://images.mixin.one/Ekf9UzoHhRRfcDLchjfVrRPYZ_71jQt306WcvgwZWwEM2BIGlHcUm_sK3Nw_mjARPwIvNB9xAzAEWJyW86pVuarPu8O5YZ0WwqTo=s128"),
        topMoverMarket("32fbceaf-0be1-3039-b721-bc6f638c7f92", "AXS", "-0.0170", 100, "https://images.mixin.one/WiJjvgFGEAHd0Fg8Z5m0eKNpO1f5Frevp2Yyu6KT09zuOZ7-t7tEcfrKVQYPcoJlAxpruILNBD5A05lvXarINxkgRPFGWWwj95Gg=s128"),
        topMoverMarket("98bbcbfb-a040-33a2-911f-a7729346b00b", "SUI", "-0.0162", 100, "https://coin-images.mixinpay.com/fe432916-83f8-4d9f-3170-acfa2d1cad00/public"),
        topMoverMarket("ced36291-082c-317d-b5b9-4be7e4965dcc", "CRV", "-0.0151", 100, "https://images.mixin.one/ZeFl04CufYhd1_DXRqnqe9xxLEGqVHDCGpDsgfHSnfNH9gYpcKwl2ELYPhLceSjDLO-iglj3pKFpiPN1y2c8QMm0YaURfXvsVH26gQ=s128"),
    )

    private fun topMoverMarket(
        id: String,
        symbol: String,
        change: String,
        leverage: Int,
        iconUrl: String,
    ) = PerpsMarket(
        marketId = id,
        displaySymbol = "$symbol-PERP",
        tokenSymbol = symbol,
        quoteSymbol = "USDT",
        markPrice = "100.00",
        leverage = leverage,
        iconUrl = iconUrl,
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
