package one.mixin.android.extension

import android.app.Application
import android.content.ContextWrapper
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import one.mixin.android.ui.home.web3.trade.perps.PerpsActivity
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PerpsLinkLoadingTest {
    @Test
    fun completeOrderLinksLeaveLocalNavigationToTheLinkBottomSheet() = runBlocking {
        for (scheme in listOf("https", "mixin")) {
            for (side in listOf("long", "short")) {
                val link = "$scheme://mixin.one/trade?type=perps&market=$marketId&action=open&side=$side&leverage=10&margin=10&leader_position=$leaderId"
                assertFalse(link.openLocalMixinTradeAction(ContextWrapper(null)))
            }
        }
    }

    @Test
    fun incompleteOrderLinksAlsoUseTheLinkBottomSheetBeforeNavigation() = runBlocking {
        for (parameters in listOf("", "&side=long", "&side=short&margin=10")) {
            val link = "https://mixin.one/trade?type=perps&market=$marketId&action=open$parameters"
            assertFalse(link.openLocalMixinTradeAction(ContextWrapper(null)))
        }
    }

    @Test
    fun addMarginLinksUseTheLinkBottomSheetBeforeNavigation() = runBlocking {
        for (scheme in listOf("https", "mixin")) {
            for (parameters in listOf("", "&margin=10")) {
                val link = "$scheme://mixin.one/trade?type=perps&market=$marketId&action=add_margin$parameters"
                assertFalse(link.openLocalMixinTradeAction(ContextWrapper(null)))
            }
        }
    }

    @Test
    fun addMarginOpensMarketDetailWithOptionalAmount() {
        val context = RuntimeEnvironment.getApplication()
        for (margin in listOf(null, "10")) {
            PerpsActivity.showDetail(context, marketId, "BTC-USDT", "BTC-USDT", "BTC", addMargin = true, initialMargin = margin)
            val intent = shadowOf(context).nextStartedActivity
            assertEquals(PerpsActivity::class.java.name, intent.component?.className)
            assertEquals(marketId, intent.getStringExtra("extra_market_id"))
            assertEquals(PerpsActivity.MODE_ADD_MARGIN, intent.getStringExtra("extra_mode"))
            assertEquals(margin, intent.getStringExtra("extra_initial_margin"))
        }
        PerpsActivity.showDetail(context, marketId, "BTC-USDT", "BTC-USDT", "BTC")
        assertEquals(PerpsActivity.MODE_DETAIL, shadowOf(context).nextStartedActivity.getStringExtra("extra_mode"))
    }

    private val marketId = "e015f42e-b0ff-38e7-87b1-7e8d46fea119"
    private val leaderId = "45d4c134-5682-4b1a-baf5-7c73b1590cc1"
}
