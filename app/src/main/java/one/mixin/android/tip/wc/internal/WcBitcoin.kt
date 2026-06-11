package one.mixin.android.tip.wc.internal

data class WcBitcoinGetAccountAddresses(
    val account: String,
    val intentions: List<String>? = null,
)

data class WcBitcoinSignMessage(
    val account: String,
    val message: String,
    val address: String? = null,
    val protocol: String? = null,
)

data class WcBitcoinAccountAddress(
    val address: String,
    val publicKey: String? = null,
    val path: String? = null,
    val intention: String = "payment",
)

data class WcBitcoinSignature(
    val address: String,
    val signature: String,
    val messageHash: String? = null,
)
