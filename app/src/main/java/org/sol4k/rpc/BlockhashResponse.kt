package org.sol4k.rpc

import kotlinx.serialization.Serializable

@Serializable
internal data class BlockhashResponse(
    val context: BlockhashContext,
    val value: BlockhashValue,
)

@Serializable
internal data class BlockhashContext(val slot: Long, val apiVersion: String)

@Serializable
internal data class BlockhashValue(
    val blockhash: String,
    val lastValidBlockHeight: Long,
)
