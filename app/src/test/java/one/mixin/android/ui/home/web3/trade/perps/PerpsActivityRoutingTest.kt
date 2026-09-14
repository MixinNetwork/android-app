package one.mixin.android.ui.home.web3.trade.perps

import one.mixin.android.extension.toPerpsTradeAction
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PerpsActivityRoutingTest {
    @Test
    fun existingPositionKeepsMarketDetail() {
        assertFalse(canOpenNewPerpsPosition(hasOpenPosition = true))
    }

    @Test
    fun missingPositionOpensNewPositionPage() {
        assertTrue(canOpenNewPerpsPosition(hasOpenPosition = false))
    }

    @Test
    fun completeLinkCanPreviewBothDirectionsWithoutLeaderId() {
        for (side in listOf("long", "short")) {
            val action = assertNotNull("$link&side=$side&leverage=10&margin=10.5".toPerpsTradeAction()?.openPosition)
            assertTrue(canPreviewPerpsLinkOrder(action.isLong, action.leverage, action.margin))
        }
    }

    @Test
    fun incompleteLinkKeepsOpenPositionPage() {
        for (parameters in listOf("", "&side=long", "&side=short&leverage=10", "&side=long&margin=10", "&leverage=10&margin=10")) {
            val action = assertNotNull("$link$parameters".toPerpsTradeAction()?.openPosition)
            assertFalse(canPreviewPerpsLinkOrder(action.isLong, action.leverage, action.margin))
        }
    }

    @Test
    fun invalidValuesCannotCreateAnOrder() {
        assertFalse(canPreviewPerpsLinkOrder(true, 0, "10"))
        assertFalse(canPreviewPerpsLinkOrder(true, -1, "10"))
        for (margin in listOf("", "invalid", "0", "-1")) {
            assertFalse(canPreviewPerpsLinkOrder(false, 10, margin))
        }
    }

    private val link = "https://mixin.one/trade?type=perps&market=e015f42e-b0ff-38e7-87b1-7e8d46fea119&action=open"

}
