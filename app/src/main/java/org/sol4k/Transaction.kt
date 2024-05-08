package org.sol4k

import org.sol4k.instruction.Instruction
import java.nio.ByteBuffer

class Transaction(
    private val recentBlockhash: String,
    private val instructions: List<Instruction>,
    private val feePayer: PublicKey,
) {
    constructor(
        recentBlockhash: String,
        instruction: Instruction,
        feePayer: PublicKey,
    ) : this(recentBlockhash, listOf(instruction), feePayer)

    private val signatures: MutableList<String> = mutableListOf()

    fun sign(keypair: Keypair) {
        val message = transactionMessage()
        val signature = keypair.sign(message)
        signatures.add(Base58.encode(signature))
    }

    private fun transactionMessage(): ByteArray {
        val accountKeys = buildAccountKeys()
        val transactionAccountPublicKeys = accountKeys.map { it.publicKey }
        val accountAddressesLength = Binary.encodeLength(accountKeys.size)
        val instructionBytes = instructions.map { instruction ->
            val keyIndices = ByteArray(instruction.keys.size) {
                transactionAccountPublicKeys.indexOf(instruction.keys[it].publicKey).toByte()
            }
            byteArrayOf(transactionAccountPublicKeys.indexOf(instruction.programId).toByte()) +
                Binary.encodeLength(instruction.keys.size) +
                keyIndices +
                Binary.encodeLength(instruction.data.size) +
                instruction.data
        }
        val instructionsLength = Binary.encodeLength(instructions.size)
        val bufferSize = HEADER_LENGTH +
            RECENT_BLOCK_HASH_LENGTH +
            accountAddressesLength.size +
            (accountKeys.size * PUBLIC_KEY_LENGTH) +
            instructionsLength.size +
            instructionBytes.sumOf { it.size }
        val buffer = ByteBuffer.allocate(bufferSize)
        val numRequiredSignatures = accountKeys.count { it.signer }.toByte()
        val numReadonlySignedAccounts = accountKeys.count { it.signer && !it.writable }.toByte()
        val numReadonlyUnsignedAccounts = accountKeys.count { !it.signer && !it.writable }.toByte()
        buffer.put(byteArrayOf(numRequiredSignatures, numReadonlySignedAccounts, numReadonlyUnsignedAccounts))
        buffer.put(accountAddressesLength)
        accountKeys.forEach { accountMeta ->
            buffer.put(accountMeta.publicKey.bytes())
        }
        buffer.put(Base58.decode(recentBlockhash))
        buffer.put(instructionsLength)
        instructionBytes.forEach { buffer.put(it) }
        return buffer.array()
    }

    fun serialize(): ByteArray {
        val signaturesLength = Binary.encodeLength(signatures.size)
        val message = this.transactionMessage()
        val buffer = ByteBuffer.allocate(
            signaturesLength.size + signatures.size * SIGNATURE_LENGTH + message.size
        )
        buffer.put(signaturesLength)
        signatures.forEach { signature ->
            buffer.put(Base58.decode(signature))
        }
        buffer.put(message)
        return buffer.array()
    }

    private fun buildAccountKeys(): List<AccountMeta> {
        val programIds = instructions
            .map { it.programId }.toSet()
        val baseAccountKeys = instructions
            .flatMap { it.keys }
            .filter { acc -> acc.publicKey != this.feePayer }
            .filter { acc -> acc.publicKey !in programIds }
        val programIdKeys = programIds
            .map { AccountMeta(it, writable = false, signer = false) }
        val feePayerList = listOf(AccountMeta(feePayer, writable = true, signer = true))
        return feePayerList + baseAccountKeys + programIdKeys
    }

    companion object {
        private const val HEADER_LENGTH = 3
        private const val RECENT_BLOCK_HASH_LENGTH = 32
        private const val PUBLIC_KEY_LENGTH = 32
        private const val SIGNATURE_LENGTH = 64
    }
}