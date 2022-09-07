package one.mixin.android.tip.exception

class NotEnoughPartialsException(
    val partialSize: Int,
) : TipNodeException() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
