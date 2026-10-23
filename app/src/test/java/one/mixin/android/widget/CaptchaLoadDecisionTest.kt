package one.mixin.android.widget

import kotlin.test.Test
import kotlin.test.assertEquals

class CaptchaLoadDecisionTest {
    @Test
    fun pageFinishedKeepsWatchingForCaptchaReadiness() {
        assertEquals(
            CaptchaLoadAction.KeepWatching,
            decideCaptchaLoadAction(
                event = CaptchaLoadEvent.PageFinished,
                captchaType = CaptchaView.CaptchaType.HCaptcha,
                attemptedCaptchaTypes = listOf(CaptchaView.CaptchaType.HCaptcha),
                failureCount = 0,
                maxFailureCount = 6,
                fallbackEnabled = true,
            ),
        )
    }

    @Test
    fun challengeReadyStopsLoadingTimeout() {
        assertEquals(
            CaptchaLoadAction.CancelWatchdog,
            decideCaptchaLoadAction(
                event = CaptchaLoadEvent.ChallengeReady,
                captchaType = CaptchaView.CaptchaType.HCaptcha,
                attemptedCaptchaTypes = listOf(CaptchaView.CaptchaType.HCaptcha),
                failureCount = 0,
                maxFailureCount = 6,
                fallbackEnabled = true,
            ),
        )
    }

    @Test
    fun googleRenderingStopsTimeoutWithoutAnOpenCallback() {
        assertEquals(
            CaptchaLoadAction.CancelWatchdog,
            decideCaptchaLoadAction(
                event = CaptchaLoadEvent.WidgetRendered,
                captchaType = CaptchaView.CaptchaType.GCaptcha,
                attemptedCaptchaTypes = listOf(CaptchaView.CaptchaType.GCaptcha),
                failureCount = 0,
                maxFailureCount = 6,
                fallbackEnabled = true,
            ),
        )
    }

    @Test
    fun failureAfterTwoServerChallengesTriesRemainingProvider() {
        CaptchaView.CaptchaType.entries.forEach { first ->
            CaptchaView.CaptchaType.entries.filter { it != first }.forEach { current ->
                val remaining = CaptchaView.CaptchaType.entries.single { it != first && it != current }
                assertEquals(
                    CaptchaLoadAction.SwitchTo(remaining),
                    decideCaptchaLoadAction(
                        event = CaptchaLoadEvent.FatalError,
                        captchaType = current,
                        attemptedCaptchaTypes = listOf(first, current),
                        failureCount = 1,
                        maxFailureCount = 6,
                        fallbackEnabled = true,
                    ),
                )
            }
        }
    }

    @Test
    fun firstFailedCycleRetriesLeastRecentlyAttemptedProvider() {
        assertEquals(
            CaptchaLoadAction.SwitchTo(CaptchaView.CaptchaType.GCaptcha),
            decideCaptchaLoadAction(
                event = CaptchaLoadEvent.FatalError,
                captchaType = CaptchaView.CaptchaType.GTCaptcha,
                attemptedCaptchaTypes = listOf(
                    CaptchaView.CaptchaType.GCaptcha,
                    CaptchaView.CaptchaType.HCaptcha,
                    CaptchaView.CaptchaType.GTCaptcha,
                ),
                failureCount = 3,
                maxFailureCount = 6,
                fallbackEnabled = true,
            ),
        )
    }

    @Test
    fun secondFailedCycleStopsFallback() {
        assertEquals(
            CaptchaLoadAction.Stop,
            decideCaptchaLoadAction(
                event = CaptchaLoadEvent.FatalError,
                captchaType = CaptchaView.CaptchaType.GTCaptcha,
                attemptedCaptchaTypes = CaptchaView.CaptchaType.entries,
                failureCount = 6,
                maxFailureCount = 6,
                fallbackEnabled = true,
            ),
        )
    }
}
