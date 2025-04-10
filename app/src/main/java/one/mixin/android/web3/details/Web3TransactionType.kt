package one.mixin.android.web3.details

enum class Web3TransactionType(val value: String) {
    TRANSFER_IN("transfer_in"),
    TRANSFER_OUT("transfer_out"),
    SWAP("swap"),
    APPROVAL("approval"),
    UNKNOWN("unknown"),
}
