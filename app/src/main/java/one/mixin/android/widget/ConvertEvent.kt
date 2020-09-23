package one.mixin.android.widget

data class ConvertEvent(val id: String, var progress: Float) {
    init {
        if (progress.isNaN()) {
            progress = 0f
        }
    }
}
