package one.mixin.android.event

import one.mixin.android.widget.CircleProgress.Companion.STATUS_ERROR

class ProgressEvent(val id: String, var progress: Float, val status: Int = STATUS_ERROR)
