package org.sol4k.rpc

import kotlinx.serialization.Serializable

@Serializable
internal data class RpcErrorResponse(
    val error: RpcError,
    val id: Long,
    val jsonrpc: String,
)
