@file:Suppress("unused")

package org.sol4k

import okio.Buffer
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow

private const val InstructionRequestUnits = 0
private const val InstructionRequestHeapFrame = 1
private const val InstructionSetComputeUnitLimit = 2
private const val InstructionSetComputeUnitPrice = 3

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
    val feeInLamports = BigDecimal(unitLimit).multiply(BigDecimal(unitPrice).divide(BigDecimal(10.0.pow(6))))
    return feeInLamports.divide(BigDecimal(10.0.pow(9))).setScale(9, RoundingMode.CEILING)
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