package org.sol4kt.instruction

data class CompiledInstruction(
    @Suppress("ArrayInDataClass") val data: ByteArray,
    val accounts: List<Int>,
    val programIdIndex: Int,
)