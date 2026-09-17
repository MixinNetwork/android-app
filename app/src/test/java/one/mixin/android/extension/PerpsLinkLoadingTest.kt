package one.mixin.android.extension

import android.app.Application
import android.content.ContextWrapper
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
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

    private val marketId = "e015f42e-b0ff-38e7-87b1-7e8d46fea119"
    private val leaderId = "45d4c134-5682-4b1a-baf5-7c73b1590cc1"
}
