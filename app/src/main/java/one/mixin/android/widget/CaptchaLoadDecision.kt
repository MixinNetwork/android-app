package one.mixin.android.widget

internal enum class CaptchaLoadEvent {
    PageFinished,
    SdkLoaded,
    WidgetRendered,
    ChallengeReady,
    FatalError,
}

internal sealed interface CaptchaLoadAction {
    data object KeepWatching : CaptchaLoadAction

    data object CancelWatchdog : CaptchaLoadAction

    data class SwitchTo(val captchaType: CaptchaView.CaptchaType) : CaptchaLoadAction

    data object Stop : CaptchaLoadAction
}

internal fun decideCaptchaLoadAction(
    event: CaptchaLoadEvent,
    captchaType: CaptchaView.CaptchaType,
    attemptedCaptchaTypes: List<CaptchaView.CaptchaType>,
    failureCount: Int,
    maxFailureCount: Int,
    fallbackEnabled: Boolean,
): CaptchaLoadAction =
    when (event) {
        CaptchaLoadEvent.PageFinished,
        CaptchaLoadEvent.SdkLoaded,
        -> CaptchaLoadAction.KeepWatching

        CaptchaLoadEvent.WidgetRendered -> {
            // reCAPTCHA has no challenge-open callback, so only time its SDK rendering.
            if (captchaType == CaptchaView.CaptchaType.GCaptcha) {
                CaptchaLoadAction.CancelWatchdog
            } else {
                CaptchaLoadAction.KeepWatching
            }
        }

        CaptchaLoadEvent.ChallengeReady -> CaptchaLoadAction.CancelWatchdog
        CaptchaLoadEvent.FatalError -> {
            if (!fallbackEnabled || failureCount >= maxFailureCount) {
                CaptchaLoadAction.Stop
            } else {
                CaptchaLoadAction.SwitchTo(
                    CaptchaView.CaptchaType.entries
                        .filter { it != captchaType }
                        .minBy { attemptedCaptchaTypes.indexOf(it) },
                )
            }
        }
    }
