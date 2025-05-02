package org.sol4kt.rpc

import kotlinx.serialization.Serializable

@Serializable
internal data class BlockhashValue(
    val blockhash: String,
    val lastValidBlockHeight: Long,
)