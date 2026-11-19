package one.mixin.android.tip.wc.internal

open class WalletConnectException(val code: Int, message: String) : RuntimeException(message) {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
