package org.sol4kt

import org.sol4k.PublicKey

data class CompiledAddressLookupTable(
    val publicKey: PublicKey,
    @Suppress("ArrayInDataClass") val writableIndexes: ByteArray,
    @Suppress("ArrayInDataClass") val readonlyIndexes: ByteArray,
)