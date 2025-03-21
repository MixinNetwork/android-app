package one.mixin.android.db.web3.vo

enum class TransactionType(val value: String) {
    TxPending("pending"),
    TxNotFound("notfound"),
    TxFailed("failed"),
    TxSuccess("success"),
}