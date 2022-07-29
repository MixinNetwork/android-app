package one.mixin.android.tip

class TipNullException(message: String) : TipException(message) {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
