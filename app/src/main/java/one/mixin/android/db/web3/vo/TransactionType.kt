package one.mixin.android.db.web3.vo

enum class TransactionType(value: String) {
    TxPending("pending"),
    TxNotFound("notfound"),
    TxFailed("failed"),
    TxSuccess("success"),
}