package one.mixin.android.web3.details

enum class Web3TransactionType(val value: String) {
    Receive("receive"),
    Send("send"),
    Other("other"),
    Contract("contract"),
}
