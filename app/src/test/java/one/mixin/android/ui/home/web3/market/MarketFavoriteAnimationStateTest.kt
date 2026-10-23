package one.mixin.android.ui.home.web3.market

import org.junit.Assert.assertEquals
import org.junit.Test

class MarketFavoriteAnimationStateTest {
    @Test
    fun initialNotFavoredSnapsToFirstFrame() {
        val state = MarketFavoriteAnimationState(initialFavored = false)

        assertDecision(state.update(isFavored = false, intent = null), MarketFavoriteAnimationMode.SNAP, 0f)
        assertDecision(state.update(isFavored = false, intent = null), MarketFavoriteAnimationMode.SNAP, 0f)
    }

    @Test
    fun initialFavoredSnapsToFinalFrame() {
        val state = MarketFavoriteAnimationState(initialFavored = true)

        assertDecision(state.update(isFavored = true, intent = null), MarketFavoriteAnimationMode.SNAP, 1f)
    }

    @Test
    fun explicitAddIntentAnimatesForward() {
        val state = MarketFavoriteAnimationState(initialFavored = false)

        assertDecision(
            state.update(false, MarketFavoriteAnimationIntent(id = 1, targetFavored = true)),
            MarketFavoriteAnimationMode.ANIMATE_FORWARD,
            1f,
        )
    }

    @Test
    fun explicitRemoveIntentSnapsToFirstFrame() {
        val state = MarketFavoriteAnimationState(initialFavored = true)

        assertDecision(
            state.update(true, MarketFavoriteAnimationIntent(id = 1, targetFavored = false)),
            MarketFavoriteAnimationMode.SNAP,
            0f,
        )
    }

    @Test
    fun repeatedIntentContinuesWithoutCreatingAnotherStart() {
        val state = MarketFavoriteAnimationState(initialFavored = false)
        val intent = MarketFavoriteAnimationIntent(id = 1, targetFavored = true)

        assertDecision(state.update(false, intent), MarketFavoriteAnimationMode.ANIMATE_FORWARD, 1f)
        assertDecision(state.update(false, intent), MarketFavoriteAnimationMode.ANIMATE_FORWARD, 1f)
        assertDecision(
            state.update(false, MarketFavoriteAnimationIntent(id = 2, targetFavored = false)),
            MarketFavoriteAnimationMode.SNAP,
            0f,
        )
    }

    @Test
    fun externalFavoriteRefreshSnapsWithoutAnimation() {
        val state = MarketFavoriteAnimationState(initialFavored = false)

        assertDecision(state.update(false, null), MarketFavoriteAnimationMode.SNAP, 0f)
        assertDecision(state.update(true, null), MarketFavoriteAnimationMode.SNAP, 1f)
        assertDecision(state.update(false, null), MarketFavoriteAnimationMode.SNAP, 0f)
    }

    @Test
    fun authoritativeConfirmationKeepsActiveIntent() {
        val state = MarketFavoriteAnimationState(initialFavored = false)
        val intent = MarketFavoriteAnimationIntent(id = 1, targetFavored = true)

        assertDecision(state.update(false, intent), MarketFavoriteAnimationMode.ANIMATE_FORWARD, 1f)
        assertDecision(state.update(true, intent), MarketFavoriteAnimationMode.ANIMATE_FORWARD, 1f)
        assertDecision(state.update(true, null), MarketFavoriteAnimationMode.SNAP, 1f)
    }

    @Test
    fun completedIntentKeepsTargetUntilCleared() {
        val state = MarketFavoriteAnimationState(initialFavored = false)
        val intent = MarketFavoriteAnimationIntent(id = 1, targetFavored = true)

        assertDecision(state.update(false, intent), MarketFavoriteAnimationMode.ANIMATE_FORWARD, 1f)
        state.onAnimationFinished(intent.id)
        assertDecision(state.update(false, intent), MarketFavoriteAnimationMode.SNAP, 1f)
        assertDecision(state.update(false, null), MarketFavoriteAnimationMode.SNAP, 0f)
    }

    @Test
    fun repeatedRemoveIntentKeepsTargetUntilCleared() {
        val state = MarketFavoriteAnimationState(initialFavored = true)
        val intent = MarketFavoriteAnimationIntent(id = 1, targetFavored = false)

        assertDecision(state.update(true, intent), MarketFavoriteAnimationMode.SNAP, 0f)
        assertDecision(state.update(true, intent), MarketFavoriteAnimationMode.SNAP, 0f)
    }

    @Test
    fun failedIntentRollsBackAfterAnimationFinishes() {
        val state = MarketFavoriteAnimationState(initialFavored = true)
        val intent = MarketFavoriteAnimationIntent(id = 1, targetFavored = false)

        assertDecision(state.update(true, intent), MarketFavoriteAnimationMode.SNAP, 0f)
        state.onAnimationFinished(intent.id)
        assertDecision(state.update(true, intent), MarketFavoriteAnimationMode.SNAP, 0f)
        assertDecision(state.update(true, null), MarketFavoriteAnimationMode.SNAP, 1f)
    }

    @Test
    fun failedForwardIntentSnapsBackToAuthoritativeState() {
        val state = MarketFavoriteAnimationState(initialFavored = false)

        assertDecision(
            state.update(false, MarketFavoriteAnimationIntent(id = 1, targetFavored = true)),
            MarketFavoriteAnimationMode.ANIMATE_FORWARD,
            1f,
        )
        assertDecision(state.update(false, null), MarketFavoriteAnimationMode.SNAP, 0f)
    }

    @Test
    fun failedRemoveIntentSnapsBackToAuthoritativeState() {
        val state = MarketFavoriteAnimationState(initialFavored = true)

        assertDecision(
            state.update(true, MarketFavoriteAnimationIntent(id = 1, targetFavored = false)),
            MarketFavoriteAnimationMode.SNAP,
            0f,
        )
        assertDecision(state.update(true, null), MarketFavoriteAnimationMode.SNAP, 1f)
    }

    @Test
    fun newerRemoveIntentCancelsForwardAnimation() {
        val state = MarketFavoriteAnimationState(initialFavored = false)

        state.update(false, MarketFavoriteAnimationIntent(id = 1, targetFavored = true))

        assertDecision(
            state.update(false, MarketFavoriteAnimationIntent(id = 2, targetFavored = false)),
            MarketFavoriteAnimationMode.SNAP,
            0f,
        )
    }

    @Test
    fun newerForwardIntentReplacesRemoveIntent() {
        val state = MarketFavoriteAnimationState(initialFavored = true)

        state.update(true, MarketFavoriteAnimationIntent(id = 1, targetFavored = false))

        assertDecision(
            state.update(true, MarketFavoriteAnimationIntent(id = 2, targetFavored = true)),
            MarketFavoriteAnimationMode.ANIMATE_FORWARD,
            1f,
        )
    }

    @Test
    fun successfulAddWaitsForAnimationCompletionBeforeClearingIntent() {
        val intent = MarketFavoriteAnimationIntent(id = 1, targetFavored = true)

        assertEquals(
            false,
            shouldClearFavoriteAnimationIntent(
                intent = intent,
                requestResult = true,
                isFavored = true,
                completedIntentId = null,
            ),
        )
        assertEquals(
            true,
            shouldClearFavoriteAnimationIntent(
                intent = intent,
                requestResult = true,
                isFavored = true,
                completedIntentId = intent.id,
            ),
        )
    }

    @Test
    fun successfulRemoveDoesNotWaitForAnimationCompletion() {
        assertEquals(
            true,
            shouldClearFavoriteAnimationIntent(
                intent = MarketFavoriteAnimationIntent(id = 1, targetFavored = false),
                requestResult = true,
                isFavored = false,
                completedIntentId = null,
            ),
        )
    }

    private fun assertDecision(
        actual: MarketFavoriteAnimationDecision,
        mode: MarketFavoriteAnimationMode,
        targetProgress: Float,
    ) {
        assertEquals(mode, actual.mode)
        assertEquals(targetProgress, actual.targetProgress)
    }
}
