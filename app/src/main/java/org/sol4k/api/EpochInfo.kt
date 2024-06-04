package org.sol4k.api

data class EpochInfo(
    val absoluteSlot: Int,
    val blockHeight: Int,
    val epoch: Int,
    val slotIndex: Int,
    val slotsInEpoch: Int,
    val transactionCount: Long,
)
