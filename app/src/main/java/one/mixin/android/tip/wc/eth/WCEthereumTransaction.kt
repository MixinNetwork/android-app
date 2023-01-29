package one.mixin.android.tip.wc.eth

data class WCEthereumTransaction(
    val from: String,
    val to: String?,
    val nonce: String?,
    val gasPrice: String?,
    val maxFeePerGas: String?,
    val maxPriorityFeePerGas: String?,
    val gas: String?,
    val gasLimit: String?,
    val value: String?,
    val data: String,
)
