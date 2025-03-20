package one.mixin.android.api.response.web3

data class Tx(
    val state: String,
)

enum class TxState {
    Pending,
    NotFound,
    Failed,
    Success,
}

fun String.isFinalTxState(): Boolean = isTxSuccess() || isTxFailed() || isNotFound()

fun String.isTxSuccess(): Boolean = this.equals(TxState.Success.name, true)

fun String.isTxFailed(): Boolean = this.equals(TxState.Failed.name, true)

fun String.isNotFound(): Boolean = this.equals(TxState.NotFound.name, true)
