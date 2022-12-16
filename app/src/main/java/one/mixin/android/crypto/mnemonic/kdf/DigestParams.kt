package one.mixin.android.crypto.mnemonic.kdf

sealed class DigestParams(val keySize: Int) {
    object Sha256 : DigestParams(256)
    object Sha512 : DigestParams(512)
}
