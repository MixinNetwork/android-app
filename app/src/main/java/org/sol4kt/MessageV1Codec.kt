package org.sol4kt

import okio.Buffer
import org.sol4k.Base58
import org.sol4k.PublicKey
import org.sol4kt.instruction.CompiledInstruction

data class TransactionConfig(
    val priorityFee: ULong? = null,
    val computeUnitLimit: UInt? = null,
    val loadedAccountsDataSizeLimit: UInt? = null,
    val heapSize: UInt? = null,
) {
    internal val mask: Int
        get() = (if (priorityFee != null) 3 else 0) or
            (if (computeUnitLimit != null) 4 else 0) or
            (if (loadedAccountsDataSizeLimit != null) 8 else 0) or
            (if (heapSize != null) 16 else 0)
}

internal object MessageV1Codec {
    const val PREFIX = 0x81
    const val MAX_TRANSACTION_SIZE = 4096

    fun serialize(message: MessageCompat): ByteArray {
        validate(message)
        val b = Buffer()
        val config = message.transactionConfig
        b.writeByte(PREFIX)
            .writeByte(message.header.numRequireSignatures)
            .writeByte(message.header.numReadonlySignedAccounts)
            .writeByte(message.header.numReadonlyUnsignedAccounts)
            .writeIntLe(config.mask)
            .write(Base58.decode(message.recentBlockhash))
            .writeByte(message.instructions.size)
            .writeByte(message.accounts.size)
        message.accounts.forEach { b.write(it.bytes()) }
        config.priorityFee?.let { b.writeLongLe(it.toLong()) }
        config.computeUnitLimit?.let { b.writeIntLe(it.toInt()) }
        config.loadedAccountsDataSizeLimit?.let { b.writeIntLe(it.toInt()) }
        config.heapSize?.let { b.writeIntLe(it.toInt()) }
        message.instructions.forEach {
            b.writeByte(it.programIdIndex).writeByte(it.accounts.size).writeShortLe(it.data.size)
        }
        message.instructions.forEach {
            it.accounts.forEach(b::writeByte)
            b.write(it.data)
        }
        require(b.size + message.header.numRequireSignatures * 64 <= MAX_TRANSACTION_SIZE) {
            "V1 transaction exceeds 4096 bytes"
        }
        return b.readByteArray()
    }

    fun deserialize(bytes: ByteArray): MessageCompat {
        require(bytes.size <= MAX_TRANSACTION_SIZE) { "V1 message exceeds 4096 bytes" }
        val b = Buffer().write(bytes)
        val message = read(b)
        require(b.exhausted()) { "Trailing data after V1 message" }
        require(bytes.size + message.header.numRequireSignatures * 64 <= MAX_TRANSACTION_SIZE) {
            "V1 transaction exceeds 4096 bytes"
        }
        return message
    }

    fun read(b: Buffer): MessageCompat {
        require(b.size >= 42) { "Truncated V1 header" }
        require(b.readU8() == PREFIX) { "Invalid V1 prefix" }
        val header = MessageHeader(b.readU8(), b.readU8(), b.readU8())
        val mask = b.readIntLe()
        require(mask and 31.inv() == 0 && (mask and 3 == 0 || mask and 3 == 3)) {
            "Invalid V1 transaction config mask"
        }
        val blockhash = Base58.encode(b.readByteArray(32))
        val instructionCount = b.readU8()
        val accountCount = b.readU8()
        require(instructionCount <= 64 && accountCount in 1..64) { "Invalid V1 account or instruction count" }
        val configSize = Integer.bitCount(mask) * 4
        require(b.size >= accountCount * 32L + configSize + instructionCount * 4L) {
            "Truncated V1 accounts, config or instruction headers"
        }
        val accounts = List(accountCount) { PublicKey(b.readByteArray(32)) }
        val config = TransactionConfig(
            priorityFee = if (mask and 3 != 0) b.readLongLe().toULong() else null,
            computeUnitLimit = if (mask and 4 != 0) b.readIntLe().toUInt() else null,
            loadedAccountsDataSizeLimit = if (mask and 8 != 0) b.readIntLe().toUInt() else null,
            heapSize = if (mask and 16 != 0) b.readIntLe().toUInt() else null,
        )
        val headers = List(instructionCount) {
            Triple(b.readU8(), b.readU8(), b.readShortLe().toInt() and 0xffff)
        }
        val instructions = headers.map { (programIndex, numAccounts, dataLength) ->
            require(b.size >= numAccounts.toLong() + dataLength) { "Truncated V1 instruction payload" }
            val indices = List(numAccounts) { b.readU8() }
            CompiledInstruction(b.readByteArray(dataLength.toLong()), indices, programIndex)
        }
        return MessageCompat(
            MessageCompat.MessageVersion.V1, header, accounts, blockhash, instructions, emptyList(), config,
        ).also(::validate)
    }

    private fun validate(message: MessageCompat) {
        require(message.version == MessageCompat.MessageVersion.V1) { "Expected V1 message" }
        require(message.addressLookupTables.isEmpty()) { "V1 does not support address lookup tables" }
        val header = message.header
        require(header.numRequireSignatures in 1..12) { "Invalid V1 signature count" }
        require(header.numReadonlySignedAccounts in 0 until header.numRequireSignatures) { "V1 requires a writable fee payer" }
        require(header.numReadonlyUnsignedAccounts >= 0 &&
            header.numRequireSignatures + header.numReadonlyUnsignedAccounts <= message.accounts.size) {
            "V1 header references more accounts than available"
        }
        require(message.accounts.size in 1..64 && message.instructions.size <= 64) { "Too many V1 accounts or instructions" }
        require(message.accounts.all { it.bytes().size == 32 }) { "Invalid V1 public key length" }
        require(message.accounts.map { it.toBase58() }.distinct().size == message.accounts.size) { "Duplicate V1 accounts" }
        require(Base58.decode(message.recentBlockhash).size == 32) { "Invalid V1 blockhash" }
        message.transactionConfig.heapSize?.let {
            require(it in 32768u..262144u && it % 1024u == 0u) { "Invalid V1 heap size" }
        }
        message.instructions.forEach {
            require(it.programIdIndex in 1 until message.accounts.size) { "Invalid V1 program index" }
            require(it.accounts.size <= 255 && it.data.size <= 65535) { "V1 instruction too large" }
            require(it.accounts.all { index -> index in message.accounts.indices }) { "Invalid V1 account index" }
        }
    }

    private fun Buffer.readU8(): Int = readByte().toInt() and 0xff
}
