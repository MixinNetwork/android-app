package one.mixin.android.util

import one.mixin.android.extension.hexStringToByteArray

fun getBitcoinTransactionVirtualSize(rawTxHex: String): Int {
    val cleanedHex: String = rawTxHex.removePrefix("0x")
    val rawTxBytes: ByteArray = cleanedHex.hexStringToByteArray()
    return getBitcoinTransactionVirtualSize(rawTxBytes)
}

fun getBitcoinTransactionVirtualSize(rawTxBytes: ByteArray): Int {
    val reader = BitcoinTxReader(rawTxBytes)
    val version: Long = reader.readUInt32()
    val witnessSize: Int = readWitnessSize(reader)
    if (version < 0L) throw IllegalArgumentException("Invalid version")
    val inputCount: Long = reader.readVarInt()
    readInputs(reader, inputCount)
    val outputCount: Long = reader.readVarInt()
    readOutputs(reader, outputCount)
    val witnessPayloadSize: Int = if (witnessSize > 0) readWitnesses(reader, inputCount) else 0
    reader.readUInt32()
    if (reader.hasRemaining()) throw IllegalArgumentException("Invalid raw transaction")
    val totalSize: Int = rawTxBytes.size
    val totalWitnessSize: Int = witnessSize + witnessPayloadSize
    val strippedSize: Int = totalSize - totalWitnessSize
    val weight: Int = strippedSize * 4 + totalWitnessSize
    return (weight + 3) / 4
}

private fun readWitnessSize(reader: BitcoinTxReader): Int {
    if (reader.peekByte(0) != 0x00.toByte()) return 0
    if (reader.peekByte(1) != 0x01.toByte()) return 0
    reader.readByte()
    reader.readByte()
    return 2
}

private fun readInputs(reader: BitcoinTxReader, inputCount: Long) {
    var i: Long = 0L
    while (i < inputCount) {
        reader.readBytes(32)
        reader.readUInt32()
        val scriptLength: Long = reader.readVarInt()
        reader.readBytes(scriptLength.toInt())
        reader.readUInt32()
        i += 1L
    }
}

private fun readOutputs(reader: BitcoinTxReader, outputCount: Long) {
    var i: Long = 0L
    while (i < outputCount) {
        reader.readInt64()
        val scriptLength: Long = reader.readVarInt()
        reader.readBytes(scriptLength.toInt())
        i += 1L
    }
}

private fun readWitnesses(reader: BitcoinTxReader, inputCount: Long): Int {
    val witnessStart: Int = reader.position
    var i: Long = 0L
    while (i < inputCount) {
        val itemCount: Long = reader.readVarInt()
        var j: Long = 0L
        while (j < itemCount) {
            val itemLength: Long = reader.readVarInt()
            reader.readBytes(itemLength.toInt())
            j += 1L
        }
        i += 1L
    }
    return reader.position - witnessStart
}

private class BitcoinTxReader(private val bytes: ByteArray) {
    var position: Int = 0
        private set

    fun hasRemaining(): Boolean {
        return position != bytes.size
    }

    fun peekByte(offset: Int): Byte {
        val index: Int = position + offset
        if (index >= bytes.size) throw IllegalArgumentException("Invalid raw transaction")
        return bytes[index]
    }

    fun readByte(): Byte {
        if (position >= bytes.size) throw IllegalArgumentException("Invalid raw transaction")
        val value: Byte = bytes[position]
        position += 1
        return value
    }

    fun readBytes(length: Int): ByteArray {
        if (length < 0) throw IllegalArgumentException("Invalid length")
        if (position + length > bytes.size) throw IllegalArgumentException("Invalid raw transaction")
        val value: ByteArray = bytes.copyOfRange(position, position + length)
        position += length
        return value
    }

    fun readUInt32(): Long {
        val b0: Long = readByte().toLong() and 0xffL
        val b1: Long = readByte().toLong() and 0xffL
        val b2: Long = readByte().toLong() and 0xffL
        val b3: Long = readByte().toLong() and 0xffL
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    fun readInt64(): Long {
        val b0: Long = readByte().toLong() and 0xffL
        val b1: Long = readByte().toLong() and 0xffL
        val b2: Long = readByte().toLong() and 0xffL
        val b3: Long = readByte().toLong() and 0xffL
        val b4: Long = readByte().toLong() and 0xffL
        val b5: Long = readByte().toLong() and 0xffL
        val b6: Long = readByte().toLong() and 0xffL
        val b7: Long = readByte().toLong() and 0xffL
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24) or (b4 shl 32) or (b5 shl 40) or (b6 shl 48) or (b7 shl 56)
    }

    fun readVarInt(): Long {
        val first: Int = readByte().toInt() and 0xff
        if (first < 0xfd) return first.toLong()
        if (first == 0xfd) return readUInt16().toLong()
        if (first == 0xfe) return readUInt32()
        return readInt64()
    }

    private fun readUInt16(): Int {
        val b0: Int = readByte().toInt() and 0xff
        val b1: Int = readByte().toInt() and 0xff
        return b0 or (b1 shl 8)
    }
}
