package one.mixin.android.tip.exception

import one.mixin.android.tip.TipNodeError

class NotEnoughPartialsException(
    val partialSize: Int,
    val forRecover: Boolean,
    val tipNodeError: TipNodeError?,
) : TipNodeException() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
