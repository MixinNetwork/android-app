package org.sol4k.rpc

import kotlinx.serialization.Serializable
import java.math.BigInteger

@Serializable
internal data class Balance(
    @Serializable(with = BigIntegerSerializer::class)
    val value: BigInteger,
)
