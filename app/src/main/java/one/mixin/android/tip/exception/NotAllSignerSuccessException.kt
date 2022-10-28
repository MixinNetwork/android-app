package one.mixin.android.tip.exception

import one.mixin.android.tip.TipNodeError

class NotAllSignerSuccessException(
    val node: String,
    private val successSignerSize: Int,
    val tipNodeError: TipNodeError?,
) : TipNodeException() {
    companion object {
        private const val serialVersionUID: Long = 2L
    }

    fun allFailure() = successSignerSize == 0
}
