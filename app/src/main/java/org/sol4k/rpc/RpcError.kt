package org.sol4k.rpc

import kotlinx.serialization.Serializable

@Serializable
internal data class RpcError(
    val code: Int,
    val message: String,
)
