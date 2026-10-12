package org.sol4kt

import org.junit.Assert.*
import org.junit.Test
import org.sol4k.Base58
import org.sol4k.Keypair
import org.sol4k.PublicKey
import org.sol4kt.instruction.CompiledInstruction
import java.util.Base64

class TransactionV1Test {
    private val zeroSignature = Base58.encode(ByteArray(64))

    // Rust SDK golden vectors mirrored from solana-go/transaction_v1_test.go at
    // 4ab4b22e9aa1ac67da59a5c1d03a3b2efff80693 (Apache-2.0).
    private fun golden(name: String): ByteArray = requireNotNull(javaClass.getResource("/solana-v1/$name.hex"))
        .readText().trim().chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun decode(bytes: ByteArray) = VersionedTransactionCompat.from(Base64.getEncoder().encodeToString(bytes))

    @Test
    fun rustGoldenTransactionsRoundTripAndVerify() {
        for (name in listOf("v1GoldenTxEmptyConfig", "v1GoldenTxFullConfig", "v1GoldenTxFeeHeap")) {
            val raw = golden(name)
            val tx = decode(raw)
            assertTrue(tx.isV1)
            assertTrue(tx.message.addressLookupTables.isEmpty())
            assertArrayEquals(raw, tx.serialize())
            val message = tx.message.serialize()
            assertArrayEquals(raw.copyOfRange(0, raw.size - 64 * tx.signatures.size), message)
            assertArrayEquals(message, MessageCompat.deserialize(message).serialize())
            tx.signatures.forEachIndexed { i, signature ->
                assertTrue(tx.message.accounts[i].verify(Base58.decode(signature), message))
            }
            // Re-sign with the same deterministic seeds used by Rust and Go.
            tx.signatures.indices.forEach { index -> tx.sign(Keypair.fromSecretKey(ByteArray(32) { (index + 1).toByte() })) }
            assertArrayEquals(raw, tx.serialize())
        }
    }

    @Test
    fun partialSigningPreservesOtherSignatureAndMessage() {
        val raw = golden("v1GoldenTxFullConfig")
        val tx = decode(raw)
        val payerSignature = tx.signatures[0]
        val message = tx.message.serialize()
        tx.signatures[1] = zeroSignature
        assertFalse(tx.allSignerSigned())
        val second = Keypair.fromSecretKey(ByteArray(32) { 2 })
        tx.sign(second)
        assertEquals(payerSignature, tx.signatures[0])
        assertEquals(tx.signatures[1], tx.signatureFor(second.publicKey.toBase58()))
        assertArrayEquals(message, tx.message.serialize())
        assertArrayEquals(raw, tx.serialize())
        assertTrue(tx.allSignerSigned())
        assertThrows(IllegalArgumentException::class.java) { tx.sign(Keypair.fromSecretKey(ByteArray(32) { 3 })) }
    }

    @Test
    fun emptySignatureListInitializesAllSignerSlots() {
        val tx = decode(golden("v1GoldenTxFullConfig"))
        val expected = tx.signatures[1]
        tx.signatures.clear()
        tx.sign(Keypair.fromSecretKey(ByteArray(32) { 2 }))
        assertEquals(listOf(zeroSignature, expected), tx.signatures)
    }

    @Test
    fun configAndFeesUseUnsignedValuesAndTotalLamports() {
        val tx = decode(golden("v1GoldenTxFullConfig"))
        assertEquals(TransactionConfig(5000uL, 200000u, 65536u, 65536u), tx.message.transactionConfig)
        assertEquals(0, "0.000005".toBigDecimal().compareTo(tx.calcPriorityFee()))
        assertEquals(0, "0.000015".toBigDecimal().compareTo(tx.calcFee(tx.message.accounts[0].toBase58())))
        assertEquals(0, tx.calcFee(tx.message.accounts[1].toBase58()).signum())
        val config = TransactionConfig(ULong.MAX_VALUE, UInt.MAX_VALUE, UInt.MAX_VALUE, 32768u)
        val unsigned = VersionedTransactionCompat(tx.message.copy(transactionConfig = config), tx.signatures)
        assertEquals(config, decode(unsigned.serialize()).message.transactionConfig)
        assertEquals(0, "18446744073.709551615".toBigDecimal().compareTo(unsigned.calcPriorityFee()))
        val absent = tx.message.copy(transactionConfig = TransactionConfig())
        val zero = tx.message.copy(transactionConfig = TransactionConfig(priorityFee = 0uL))
        assertEquals(absent.serialize().size + 8, zero.serialize().size)
        assertNull(MessageCompat.deserialize(absent.serialize()).transactionConfig.priorityFee)
        assertEquals(0uL, MessageCompat.deserialize(zero.serialize()).transactionConfig.priorityFee)
    }

    @Test
    fun externalV1ConfigIsNotRewrittenAndComputeInstructionsDoNotSetFees() {
        val tx = decode(golden("v1GoldenTxFullConfig"))
        tx.signatures.replaceAll { zeroSignature }
        val before = tx.serialize()
        assertFalse(tx.setPriorityFee("20000", "600000"))
        assertArrayEquals(before, tx.serialize())
        tx.message.accounts = tx.message.accounts + org.sol4k.Constants.COMPUTE_BUDGET_PROGRAM_ID
        tx.message.instructions = tx.message.instructions + CompiledInstruction(byteArrayOf(3, 1), emptyList(), 6)
        assertEquals(0, "0.000005".toBigDecimal().compareTo(tx.calcPriorityFee()))
    }

    @Test
    fun rejectsTruncationTrailingBytesMasksAndUnknownVersions() {
        val raw = golden("v1GoldenTxFullConfig")
        for (length in raw.indices) {
            assertThrows("length=$length", Exception::class.java) { decode(raw.copyOf(length)) }
        }
        assertThrows(IllegalArgumentException::class.java) { decode(raw + 0.toByte()) }
        for (mask in listOf(1, 2, 32, 255)) {
            assertThrows(IllegalArgumentException::class.java) { decode(raw.clone().apply { this[4] = mask.toByte() }) }
        }
        assertThrows(IllegalArgumentException::class.java) { decode(raw.clone().apply { this[0] = 0x82.toByte() }) }
        assertThrows(IllegalArgumentException::class.java) { MessageCompat.deserialize(byteArrayOf(0x82.toByte())) }
        assertThrows(IllegalArgumentException::class.java) {
            decode(byteArrayOf(1) + ByteArray(64) + decode(raw).message.serialize())
        }
    }

    @Test
    fun rejectsInvalidHeaderAccountsHeapAndInstructionIndices() {
        val message = decode(golden("v1GoldenTxFullConfig")).message
        val invalid = listOf(
            message.copy(header = MessageHeader(0, 0, 0)),
            message.copy(header = MessageHeader(13, 0, 0)),
            message.copy(header = MessageHeader(2, 2, 3)),
            message.copy(header = MessageHeader(2, 1, 5)),
            message.copy(accounts = message.accounts + message.accounts.first()),
            message.copy(transactionConfig = TransactionConfig(heapSize = 32769u)),
            message.copy(transactionConfig = TransactionConfig(heapSize = 0u)),
            message.copy(transactionConfig = TransactionConfig(heapSize = 263168u)),
            message.copy(instructions = listOf(CompiledInstruction(byteArrayOf(), emptyList(), 0))),
            message.copy(instructions = listOf(CompiledInstruction(byteArrayOf(), listOf(6), 4))),
            message.copy(instructions = listOf(CompiledInstruction(byteArrayOf(), emptyList(), 6))),
            message.copy(instructions = List(65) { message.instructions.first() }),
            message.copy(addressLookupTables = listOf(CompiledAddressLookupTable(message.accounts[0], byteArrayOf(0), byteArrayOf()))),
        )
        invalid.forEach { assertThrows(IllegalArgumentException::class.java) { it.serialize() } }
    }

    @Test
    fun accepts4096BytesAndRejects4097Bytes() {
        val original = decode(golden("v1GoldenTxEmptyConfig"))
        val fixed = original.message.copy(instructions = listOf(CompiledInstruction(byteArrayOf(), listOf(0), 3)))
        val available = 4096 - fixed.serialize().size - 64
        val message = fixed.copy(instructions = listOf(CompiledInstruction(ByteArray(available), listOf(0), 3)))
        val tx = VersionedTransactionCompat(message, mutableListOf(zeroSignature))
        assertEquals(4096, tx.serialize().size)
        assertArrayEquals(tx.serialize(), decode(tx.serialize()).serialize())
        assertThrows(IllegalArgumentException::class.java) { decode(tx.serialize() + 0.toByte()) }
        message.instructions = listOf(CompiledInstruction(ByteArray(available + 1), listOf(0), 3))
        assertThrows(IllegalArgumentException::class.java) { tx.serialize() }
    }

    @Test
    fun legacyAndV0StillRoundTripAndSign() {
        val signer = Keypair.fromSecretKey(ByteArray(32) { 1 })
        for (version in listOf(MessageCompat.MessageVersion.Legacy, MessageCompat.MessageVersion.V0)) {
            val message = MessageCompat(version, MessageHeader(1, 0, 1),
                listOf(signer.publicKey, PublicKey(ByteArray(32))), Base58.encode(ByteArray(32) { 4 }),
                listOf(CompiledInstruction(byteArrayOf(2, 0, 0, 0), listOf(0), 1)),
                if (version == MessageCompat.MessageVersion.V0) {
                    listOf(CompiledAddressLookupTable(PublicKey(ByteArray(32) { 5 }), byteArrayOf(0, 1), byteArrayOf(2)))
                } else emptyList())
            val tx = VersionedTransactionCompat(message, mutableListOf(zeroSignature))
            tx.sign(signer)
            assertFalse(tx.isV1)
            assertArrayEquals(tx.serialize(), decode(tx.serialize()).serialize())
            assertTrue(signer.publicKey.verify(Base58.decode(tx.signatures[0]), message.serialize()))
        }
    }
}
