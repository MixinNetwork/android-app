package one.mixin.android.crypto

data class CryptoWallet(
    val mnemonic: String,
    val privateKey: String,
    val address: String,
    val path: String,
)