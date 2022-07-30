package one.mixin.android.tip

class TipNetWorkException(message: String, code: Int) : TipException(message) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
