package one.mixin.android.crypto

data class EdKeyPair(
    val publicKey: ByteArray,
    val privateKey: ByteArray, // 32 bytes
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EdKeyPair

        if (!publicKey.contentEquals(other.publicKey)) return false
        return privateKey.contentEquals(other.privateKey)
    }

    override fun hashCode(): Int {
        var result = publicKey.contentHashCode()
        result = 31 * result + privateKey.contentHashCode()
        return result
    }
}
