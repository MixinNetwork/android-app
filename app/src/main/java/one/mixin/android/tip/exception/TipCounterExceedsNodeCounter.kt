package one.mixin.android.tip.exception

class TipCounterExceedsNodeCounter(message: String) : TipException(message) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
