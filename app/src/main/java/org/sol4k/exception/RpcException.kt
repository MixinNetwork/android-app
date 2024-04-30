package org.sol4k.exception

import java.lang.RuntimeException

data class RpcException(
    val code: Int,
    override val message: String,
    val rawResponse: String,
) : RuntimeException(message)
