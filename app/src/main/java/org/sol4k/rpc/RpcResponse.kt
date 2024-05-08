package org.sol4k.rpc

import kotlinx.serialization.Serializable

@Serializable
internal data class RpcResponse<T>(
    val result: T,
    val id: Long,
    val jsonrpc: String,
)
