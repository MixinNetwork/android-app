package one.mixin.android.ui.wallet.transfer

import android.app.Activity
import one.mixin.android.ui.home.web3.trade.TradeFragment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class BalanceErrorTradeNavigatorTest {
    @Test
    fun showSwapTradeOpensSimpleSwapTab() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

        BalanceErrorTradeNavigator.showSwapTrade(
            context = activity,
            input = "input-asset",
            output = "output-asset",
            inMixin = false,
            walletId = "wallet-id",
        )

        val intent = shadowOf(activity).nextStartedActivity
        assertEquals("input-asset", intent.getStringExtra(TradeFragment.ARGS_INPUT))
        assertEquals("output-asset", intent.getStringExtra(TradeFragment.ARGS_OUTPUT))
        assertFalse(intent.getBooleanExtra(TradeFragment.ARGS_IN_MIXIN, true))
        assertEquals("wallet-id", intent.getStringExtra(TradeFragment.ARGS_WALLET_ID))
        assertTrue(intent.hasExtra(TradeFragment.ARGS_INITIAL_TAB))
        assertEquals(
            TradeFragment.TAB_SIMPLE,
            intent.getIntExtra(TradeFragment.ARGS_INITIAL_TAB, TradeFragment.TAB_PERPETUAL),
        )
    }
}
