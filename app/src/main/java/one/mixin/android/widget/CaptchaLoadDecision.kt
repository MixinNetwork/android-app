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

    data object RestartWatchdog : CaptchaLoadAction

    data class SwitchTo(val captchaType: CaptchaView.CaptchaType) : CaptchaLoadAction

    data object Stop : CaptchaLoadAction
}

internal fun decideCaptchaLoadAction(
    event: CaptchaLoadEvent,
    captchaType: CaptchaView.CaptchaType,
    failureCount: Int,
    maxFailureCount: Int,
    fallbackEnabled: Boolean,
): CaptchaLoadAction =
    when (event) {
        CaptchaLoadEvent.PageFinished -> CaptchaLoadAction.KeepWatching
        CaptchaLoadEvent.SdkLoaded,
        CaptchaLoadEvent.WidgetRendered,
        -> CaptchaLoadAction.RestartWatchdog

        CaptchaLoadEvent.ChallengeReady -> CaptchaLoadAction.RestartWatchdog
        CaptchaLoadEvent.FatalError -> {
            if (!fallbackEnabled || failureCount >= maxFailureCount) {
                CaptchaLoadAction.Stop
            } else {
                CaptchaLoadAction.SwitchTo(captchaType.fallback())
            }
        }
    }
