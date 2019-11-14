package one.mixin.android.event

import one.mixin.android.widget.CircleProgress

class ProgressEvent private constructor(val id: String, var progress: Float, val status: Int) {
    companion object {
        fun loadingEvent(id: String, progress: Float) = ProgressEvent(
            id, progress,
            CircleProgress.STATUS_LOADING
        )

        fun playEvent(id: String, progress: Float = 0f) =
            ProgressEvent(id, progress, CircleProgress.STATUS_PLAY)

        fun pauseEvent(id: String) =
            ProgressEvent(id, -1f, CircleProgress.STATUS_PAUSE)

        fun errorEvent(id: String): ProgressEvent =
            ProgressEvent(id, -1f, CircleProgress.STATUS_ERROR)
    }
}
