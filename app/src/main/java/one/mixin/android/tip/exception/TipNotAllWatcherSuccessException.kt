package one.mixin.android.tip.exception

class TipNotAllWatcherSuccessException(
    val info: String,
) : TipException(info) {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
