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
                failureCount = 0,
                maxFailureCount = 6,
                fallbackEnabled = true,
            ),
        )
    }

    @Test
    fun challengeReadyKeepsWatchingForTerminalEvent() {
        assertEquals(
            CaptchaLoadAction.RestartWatchdog,
            decideCaptchaLoadAction(
                event = CaptchaLoadEvent.ChallengeReady,
                captchaType = CaptchaView.CaptchaType.HCaptcha,
                failureCount = 0,
                maxFailureCount = 6,
                fallbackEnabled = true,
            ),
        )
    }

    @Test
    fun hCaptchaFatalErrorSwitchesToGeeTest() {
        assertEquals(
            CaptchaLoadAction.SwitchTo(CaptchaView.CaptchaType.GTCaptcha),
            decideCaptchaLoadAction(
                event = CaptchaLoadEvent.FatalError,
                captchaType = CaptchaView.CaptchaType.HCaptcha,
                failureCount = 2,
                maxFailureCount = 6,
                fallbackEnabled = true,
            ),
        )
    }

    @Test
    fun firstFailedCycleContinuesWithNextCaptcha() {
        assertEquals(
            CaptchaLoadAction.SwitchTo(CaptchaView.CaptchaType.GCaptcha),
            decideCaptchaLoadAction(
                event = CaptchaLoadEvent.FatalError,
                captchaType = CaptchaView.CaptchaType.GTCaptcha,
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
                failureCount = 6,
                maxFailureCount = 6,
                fallbackEnabled = true,
            ),
        )
    }
}
