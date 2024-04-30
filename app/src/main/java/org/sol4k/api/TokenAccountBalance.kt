package org.sol4k.api

import java.math.BigInteger

data class TokenAccountBalance(
    val amount: BigInteger,
    val decimals: Int,
    val uiAmount: String,
)
