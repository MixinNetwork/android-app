package one.mixin.android.db.web3.vo

enum class TransactionType(val value: String) {
    TRANSFER_IN("transfer_in"),
    TRANSFER_OUT("transfer_out"),
    SWAP("swap"),
    APPROVAL("approval"),
    UNKNOWN("unknown"),
}

enum class TransactionStatus(val value: String) {
    SUCCESS("success"),
    PENDING("pending"),
    FAILED("failed"),
    NOT_FOUND("notfound"),
}