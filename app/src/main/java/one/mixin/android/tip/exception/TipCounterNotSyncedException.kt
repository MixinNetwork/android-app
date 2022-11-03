package one.mixin.android.tip.exception

class TipCounterNotSyncedException(message: String = "Tip counter not synced") : TipException(message) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
