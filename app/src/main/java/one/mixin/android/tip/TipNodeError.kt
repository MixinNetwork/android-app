package one.mixin.android.tip

sealed class TipNodeError(
    open val signerIndex: Int,
    open val requestId: String,
    open val code: Int,
    val level: Int
) {
    fun notRetry() = this is TooManyRequestError || this is IncorrectPinError
}
data class ServerError(
    override val signerIndex: Int,
    override val requestId: String,
    override val code: Int
) : TipNodeError(signerIndex, requestId, code, 0)
data class TooManyRequestError(
    override val signerIndex: Int,
    override val requestId: String,
    override val code: Int
) : TipNodeError(signerIndex, requestId, code, 1)
data class IncorrectPinError(
    override val signerIndex: Int,
    override val requestId: String,
    override val code: Int
) : TipNodeError(signerIndex, requestId, code, 2)
data class OtherTipNodeError(
    override val signerIndex: Int,
    override val requestId: String,
    override val code: Int,
    val message: String?
) : TipNodeError(signerIndex, requestId, code, 3)

internal fun Int.toTipNodeError(signerIndex: Int, requestId: String, message: String?) = when (this) {
    500 -> ServerError(signerIndex, requestId, this)
    429 -> TooManyRequestError(signerIndex, requestId, this)
    403 -> IncorrectPinError(signerIndex, requestId, this)
    else -> OtherTipNodeError(signerIndex, requestId, this, message)
}

internal fun List<TipNodeError>.getRepresentative(): TipNodeError? {
    if (isNullOrEmpty()) return null

    return minByOrNull { it.level }
}
