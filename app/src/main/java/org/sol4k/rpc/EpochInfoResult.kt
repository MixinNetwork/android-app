package org.sol4k.rpc

import kotlinx.serialization.Serializable

@Serializable
internal data class EpochInfoResult(
    val absoluteSlot: Int,
    val blockHeight: Int,
    val epoch: Int,
    val slotIndex: Int,
    val slotsInEpoch: Int,
    val transactionCount: Long,
)
