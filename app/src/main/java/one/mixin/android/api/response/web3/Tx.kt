package one.mixin.android.api.response.web3

data class Tx(
    val state: String,
)

enum class TxState {
    NotFound, Failed, Success
}

fun String.isFinalTxState(): Boolean =
    this.equals(TxState.Failed.name, true) || this.equals(TxState.Success.name, true)