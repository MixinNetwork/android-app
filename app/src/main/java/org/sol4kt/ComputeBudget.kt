@file:Suppress("unused")

package org.sol4kt

import okio.Buffer
import org.sol4k.Convert.lamportToSol
import org.sol4k.Convert.microToLamport
import java.math.BigDecimal

private const val InstructionRequestUnits = 0
private const val InstructionRequestHeapFrame = 1
const val InstructionSetComputeUnitLimit = 2
const val InstructionSetComputeUnitPrice = 3

internal fun computeBudget(data: List<ByteArray>): BigDecimal {
    if (data.size != 2) return BigDecimal.ZERO

    var unitLimit = 0
    var unitPrice = 0L
    data.map {
        val d = Buffer()
        d.write(it)
        val instruction = d.readByte().toInt()
        when (instruction) {
            InstructionSetComputeUnitLimit -> {
                unitLimit = d.readIntLe()
            }
            InstructionSetComputeUnitPrice -> {
                // micro-lamports
                unitPrice = d.readLongLe()
            }
            else -> {
                return BigDecimal.ZERO
            }
        }
    }
    if (unitLimit == 0 || unitPrice == 0L) {
        return BigDecimal.ZERO
    }
    val feeInLamports = microToLamport(BigDecimal(unitLimit).multiply(BigDecimal(unitPrice)))
    return lamportToSol(feeInLamports)
}

internal fun decodeComputeUnitLimit(data: ByteArray): Int {
    val d = Buffer()
    d.write(data)
    val instruction = d.readByte().toInt()
    if (instruction != InstructionSetComputeUnitLimit) {
        return 0
    }
    return d.readIntLe()
}

// compute unit price in "micro-lamports"
internal fun decodeComputeUnitPrice(data: ByteArray): Long {
    val d = Buffer()
    d.write(data)
    val instruction = d.readByte().toInt()
    if (instruction != InstructionSetComputeUnitPrice) {
        return 0L
    }
    return d.readLongLe()
}

internal fun notComputeInstruction(data: ByteArray): Boolean {
    val d = Buffer()
    d.write(data)
    val instruction = d.readByte().toInt()
    return instruction == InstructionRequestUnits || instruction == InstructionRequestHeapFrame
}