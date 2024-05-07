package org.sol4k.instruction

data class CompiledInstruction(
    @Suppress("ArrayInDataClass") val data: ByteArray,
    val accounts: List<Int>,
    val programIdIndex: Int,
)