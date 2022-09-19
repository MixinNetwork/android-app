package one.mixin.android.tip.exception

class NotAllSignerSuccessException(
    val successSignerSize: Int,
) : TipNodeException() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    fun allFailure() = successSignerSize == 0
}
