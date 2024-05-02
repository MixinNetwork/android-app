package one.mixin.android.web3.details

enum class Web3TransactionType(val value: String) {
    Approve("approve"),
    Borrow("borrow"),
    Burn("burn"),
    Cancel("cancel"),
    Claim("claim"),
    Deploy("deploy"),
    Deposit("deposit"),
    Execute("execute"),
    Mint("mint"),
    Receive("receive"),
    Repay("repay"),
    Send("send"),
    Stake("stake"),
    Trade("trade"),
    Unstake("unstake"),
    Withdraw("withdraw")
}
