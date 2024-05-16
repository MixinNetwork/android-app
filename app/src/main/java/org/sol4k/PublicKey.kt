package org.sol4k

import org.sol4k.Constants.ASSOCIATED_TOKEN_PROGRAM_ID
import org.sol4k.Constants.TOKEN_PROGRAM_ID
import org.sol4k.tweetnacl.TweetNaclFast
import org.sol4k.tweetnacl.TweetNaclFast.Signature
import java.nio.ByteBuffer
import java.security.MessageDigest

class PublicKey {
    private val bytes: ByteArray

    constructor(bytes: ByteArray) {
        this.bytes = bytes
    }

    constructor(publicKey: String) {
        this.bytes = Base58.decode(publicKey)
    }

    fun bytes(): ByteArray = bytes.copyOf()

    fun toBase58(): String = Base58.encode(this.bytes)

    fun verify(signature: ByteArray, message: ByteArray): Boolean =
        Signature(bytes, ByteArray(0)).detached_verify(message, signature)

    fun isOnCurve(): Boolean =
        TweetNaclFast.isOnCurve(bytes)

    override fun toString(): String = toBase58()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PublicKey

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    companion object {
        private fun createProgramAddress(seeds: List<ByteArray>, programId: PublicKey): PublicKey {
            val programDerivedAddressLabel = "ProgramDerivedAddress".toByteArray()
            val buffer = ByteBuffer.allocate(
                seeds.sumOf { it.size } +
                    programId.bytes.size +
                    programDerivedAddressLabel.size
            )
            seeds.forEach { buffer.put(it) }
            buffer.put(programId.bytes)
            buffer.put(programDerivedAddressLabel)
            val hash = MessageDigest.getInstance("SHA-256").digest(buffer.array())
            if (!TweetNaclFast.isOnCurve(hash)) {
                return PublicKey(hash)
            } else {
                throw IllegalArgumentException("Invalid seeds")
            }
        }

        @JvmStatic
        fun findProgramAddress(
            seeds: List<PublicKey>,
            programId: PublicKey,
        ): ProgramDerivedAddress {
            val seedsBinary = seeds.map { it.bytes }
            for (nonce in 255 downTo 1) try {
                val newSeeds = seedsBinary + byteArrayOf(nonce.toByte())
                val address = createProgramAddress(newSeeds, programId)
                return ProgramDerivedAddress(address, nonce)
            } catch (e: Exception) { /* ignore */
            }
            throw RuntimeException("Unable to find program address")
        }

        @JvmStatic
        fun findProgramDerivedAddress(
            holderAddress: PublicKey,
            tokenMintAddress: PublicKey,
        ): ProgramDerivedAddress = findProgramAddress(
            listOf(holderAddress, TOKEN_PROGRAM_ID, tokenMintAddress),
            ASSOCIATED_TOKEN_PROGRAM_ID,
        )
    }
}
