package one.mixin.android.event

import one.mixin.android.widget.CircleProgress.Companion.STATUS_DONE

class ProgressEvent(val id: String, var progress: Float, val status: Int = STATUS_DONE)
