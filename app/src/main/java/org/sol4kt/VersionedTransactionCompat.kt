package org.sol4kt

import okio.Buffer
import org.sol4k.Base58
import org.sol4k.Binary
import org.sol4k.Constants
import org.sol4k.Convert.lamportToSol
import org.sol4k.Keypair
import org.sol4kt.MessageCompat.MessageVersion
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Base64

class VersionedTransactionCompat(
    val message: MessageCompat,
    val signatures: MutableList<String>,
) {

    val isV1: Boolean get() = message.version == MessageVersion.V1

    fun sign(keypair: Keypair) {
        val data = message.serialize()
        val index = message.accounts.take(message.header.numRequireSignatures).indexOf(keypair.publicKey)
        require(index >= 0) { "Account is not a required Solana signer" }
        if (signatures.isEmpty()) {
            repeat(message.header.numRequireSignatures) { signatures.add(Base58.encode(ByteArray(SIGNATURE_LENGTH))) }
        }
        require(signatures.size == message.header.numRequireSignatures) { "Invalid signature count" }
        val signature = keypair.sign(data)
        signatures[index] = Base58.encode(signature)
    }

    fun signatureFor(address: String): String {
        val index = message.accounts.take(message.header.numRequireSignatures).indexOfFirst { it.toBase58() == address }
        require(index >= 0 && index < signatures.size) { "Account is not a required Solana signer" }
        return signatures[index]
    }

    fun serialize(): ByteArray {
        if (signatures.isEmpty() || signatures.size != message.header.numRequireSignatures) {
            throw Exception("Signature verification failed")
        }

        val messageData = message.serialize()

        return when (message.version) {
            MessageVersion.Legacy, MessageVersion.V0 -> Buffer().apply {
                write(Binary.encodeLength(signatures.size))
                writeSignatures()
                write(messageData)
            }
            MessageVersion.V1 -> Buffer().apply {
                write(messageData)
                writeSignatures()
            }
        }.readByteArray()
    }

    private fun Buffer.writeSignatures() {
        for (signature in signatures) {
            val bytes = Base58.decode(signature)
            require(bytes.size == SIGNATURE_LENGTH) { "Invalid Solana signature length" }
            write(bytes)
        }
    }

    fun setPriorityFee(unitPrice: String, unitLimit: String): Boolean {
        if (hasAnySignature()) return false
        return when (message.version) {
            MessageVersion.Legacy, MessageVersion.V0 -> setComputeBudgetPriorityFee(unitPrice, unitLimit)
            MessageVersion.V1 -> {
                val limit = unitLimit.toUInt()
                setV1PriorityFee(calculatePriorityFeeLamports(unitPrice, limit), limit)
            }
        }
    }

    private fun calculatePriorityFeeLamports(unitPrice: String, unitLimit: UInt): ULong {
        val price = unitPrice.toBigInteger()
        require(price.signum() >= 0 && price.bitLength() <= 64) { "Invalid compute unit price" }
        return price.toBigDecimal()
            .multiply(unitLimit.toLong().toBigDecimal())
            .divide(BigDecimal(1_000_000), 0, RoundingMode.CEILING)
            .toPlainString().toULong()
    }

    fun setV1PriorityFee(totalLamports: ULong, computeUnitLimit: UInt? = null): Boolean {
        require(message.version == MessageVersion.V1) { "setV1PriorityFee requires a V1 transaction" }
        if (hasAnySignature()) return false

        val config = message.transactionConfig.copy(
            priorityFee = totalLamports,
            computeUnitLimit = computeUnitLimit ?: message.transactionConfig.computeUnitLimit,
        )
        // Newly present config fields grow the transaction; validate before changing the message.
        message.copy(transactionConfig = config).serialize()
        message.transactionConfig = config
        return true
    }

    private fun hasAnySignature(): Boolean {
        val voidSig = Base58.encode(ByteArray(SIGNATURE_LENGTH))
        return signatures.any { it != voidSig }
    }

    private fun setComputeBudgetPriorityFee(unitPrice: String, unitLimit: String): Boolean {
        message.setPriorityFee(unitPrice.toLong(), unitLimit.toInt())
        // TODO check tx exceed max size?
        return true
    }

    fun calcFee(address: String): BigDecimal {
        if (!onlyOneSigner() && message.accounts.first().toBase58() != address ) {
            return BigDecimal.ZERO
        }
        val sigFee = lamportToSol(BigDecimal(5000 * message.header.numRequireSignatures))
        val priorityFee = calcPriorityFee()
        return sigFee.add(priorityFee).setScale(9, RoundingMode.CEILING)
    }

    fun calcPriorityFee(): BigDecimal = when (message.version) {
        MessageVersion.Legacy, MessageVersion.V0 -> calcComputeBudgetPriorityFee()
        MessageVersion.V1 -> lamportToSol((message.transactionConfig.priorityFee ?: 0uL).toString().toBigDecimal())
    }

    private fun calcComputeBudgetPriorityFee(): BigDecimal {
        val accounts = message.accounts
        val data = mutableListOf<ByteArray>()
        for (i in message.instructions) {
            if (accounts[i.programIdIndex] != Constants.COMPUTE_BUDGET_PROGRAM_ID) {
                continue
            }
            data.add(i.data)
        }
        return computeBudget(data)
    }

    fun onlyOneSigner(): Boolean {
        return message.header.numRequireSignatures == 1
    }

    fun allSignerSigned(): Boolean {
        return message.header.numRequireSignatures == signatures.size &&
                signatures.all { s -> s != Base58.encode(ByteArray(SIGNATURE_LENGTH)) }
    }

    private fun parseSystemProgramData(data: ByteArray): Long? {
        val d = Buffer()
        d.write(data)
        val instruction = d.readIntLe()
        if (instruction != 2) {
            return null
        }
        return  d.readLongLe()
    }

    private fun parseTokenProgramData(data: ByteArray): Long? {
        val d = Buffer()
        d.write(data)
        val instruction = d.readByte().toInt()
        if (instruction != 12) {
            return null
        }
        return d.readLongLe()
    }

    data class TokenBalanceChange(
        var change: Long,
        val mint: String,
    )

    companion object {
        const val PUBLIC_KEY_LENGTH = 32
        private const val SIGNATURE_LENGTH = 64

        @JvmStatic
        fun from(encodedTransaction: String): VersionedTransactionCompat {
            val byteArray = Base64.getDecoder().decode(encodedTransaction)
            require(byteArray.isNotEmpty()) { "Empty Solana transaction" }
            return when (byteArray[0].toInt() and 0xff) {
                MessageV1Codec.PREFIX -> deserializeV1(byteArray)
                in 0 until 0x80 -> deserializeLegacyOrV0(byteArray)
                else -> throw IllegalArgumentException("Invalid Solana transaction discriminator")
            }
        }

        private fun deserializeV1(bytes: ByteArray): VersionedTransactionCompat {
            require(bytes.size <= MessageV1Codec.MAX_TRANSACTION_SIZE) { "V1 transaction exceeds 4096 bytes" }
            val buffer = Buffer().write(bytes)
            val message = MessageV1Codec.read(buffer)
            require(buffer.size == message.header.numRequireSignatures * SIGNATURE_LENGTH.toLong()) {
                "Invalid V1 signature tail length"
            }
            val signatures = MutableList(message.header.numRequireSignatures) {
                Base58.encode(buffer.readByteArray(SIGNATURE_LENGTH.toLong()))
            }
            return VersionedTransactionCompat(message, signatures)
        }

        private fun deserializeLegacyOrV0(bytes: ByteArray): VersionedTransactionCompat {
            var byteArray = bytes
            val signaturesCount = BinaryCompat.decodeLength(byteArray)
            byteArray = signaturesCount.second
            val signatures = mutableListOf<String>()
            for (i in 0 until signaturesCount.first) {
                val signature = byteArray.slice(0 until SIGNATURE_LENGTH)
                byteArray = byteArray.drop(SIGNATURE_LENGTH).toByteArray()
                val encodedSignature = Base58.encode(signature.toByteArray())
                signatures.add(encodedSignature)
            }

            val message = MessageCompat.deserialize(byteArray)
            require(message.version != MessageVersion.V1) { "V1 message in legacy transaction envelope" }

            if(signaturesCount.first > 0 && message.header.numRequireSignatures != signaturesCount.first) {
                throw Exception("numRequireSignatures is not equal to signatureCount")
            }
            return VersionedTransactionCompat(message, signatures)
        }
    }
}
