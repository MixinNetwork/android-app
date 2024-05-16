package org.sol4k.rpc

import kotlinx.serialization.Serializable

@Serializable
internal data class GetTokenApplyResponse(
    val value: TokenAmount? = null,
)

@Serializable
data class TokenAmount(
    val amount: Long,
    val decimals: Int,
    val uiAmountString: String,
)