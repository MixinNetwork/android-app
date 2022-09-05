package one.mixin.android.tip

abstract class TipNodeException : Exception() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

class NotEnoughPartialsException(
    val partialSize: Int,
) : TipNodeException() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

class NotAllSignerSuccessException(
    val successSignerSize: Int,
) : TipNodeException() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }

    fun allFailure() = successSignerSize == 0
}

class DifferentIdentityException : TipNodeException() {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
