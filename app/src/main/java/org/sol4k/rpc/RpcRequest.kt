package org.sol4k.rpc

import kotlinx.serialization.Serializable

@Serializable
internal data class RpcRequest<T : Any>(
    val method: String,
    val params: List<T>,
    val jsonrpc: String,
    val id: Long,
) {
    constructor(method: String, params: List<T>) : this(
        method,
        params,
        jsonrpc = "2.0",
        id = System.currentTimeMillis()
    )
}
