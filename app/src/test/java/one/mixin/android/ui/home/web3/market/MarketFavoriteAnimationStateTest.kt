package one.mixin.android.ui.home.web3.market

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketFavoriteAnimationStateTest {
    @Test
    fun externalFavoriteUpdateDoesNotAnimate() {
        val state =
            MarketFavoriteAnimationState(
                initialAnimationTrigger = 0,
            )

        assertFalse(state.shouldPlay(animationTrigger = 0))
        assertFalse(state.shouldPlay(animationTrigger = 0))
    }

    @Test
    fun explicitFavoriteTriggerAnimates() {
        val state =
            MarketFavoriteAnimationState(
                initialAnimationTrigger = 0,
            )

        assertTrue(state.shouldPlay(animationTrigger = 1))
    }
}
