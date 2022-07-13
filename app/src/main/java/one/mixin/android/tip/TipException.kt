package one.mixin.android.tip

abstract class TipException : Exception() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

class PinIncorrectException : TipException() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
