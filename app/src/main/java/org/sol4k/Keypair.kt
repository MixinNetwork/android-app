package org.sol4k

import org.sol4k.tweetnacl.TweetNaclFast

class Keypair private constructor(
    private val keypair: TweetNaclFast.Signature.KeyPair,
) {

    val secret: ByteArray
        get() = keypair.secretKey

    val publicKey: PublicKey
        get() = PublicKey(keypair.publicKey)

    fun sign(message: ByteArray): ByteArray =
        TweetNaclFast.Signature(ByteArray(0), secret).detached(message)

    companion object {
        @JvmStatic
        fun generate(): Keypair = Keypair(TweetNaclFast.Signature.keyPair())

        @JvmStatic
        fun fromSecretKey(secret: ByteArray): Keypair {
            return Keypair(TweetNaclFast.Signature.keyPair_fromSeed(secret))
        }
    }
}
