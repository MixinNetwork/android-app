package org.sol4k

data class CompiledAddressLookupTable(
    val publicKey: PublicKey,
    @Suppress("ArrayInDataClass") val writableIndexes: ByteArray,
    @Suppress("ArrayInDataClass") val readonlyIndexes: ByteArray,
)