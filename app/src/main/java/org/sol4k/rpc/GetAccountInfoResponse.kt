package org.sol4k.rpc

import kotlinx.serialization.Serializable
import java.math.BigInteger

@Serializable
internal data class GetAccountInfoResponse(
    val value: GetAccountInfoValue? = null,
)

@Serializable
internal data class GetAccountInfoValue(
    val data: List<String>,
    val executable: Boolean,
    @Serializable(with = BigIntegerSerializer::class)
    val lamports: BigInteger,
    val owner: String,
    @Serializable(with = BigIntegerSerializer::class)
    val rentEpoch: BigInteger,
    val space: Int? = null,
)
