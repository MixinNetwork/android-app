package one.mixin.android.tip

class TipNetworkException(message: String, code: Int) : TipException(message) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
