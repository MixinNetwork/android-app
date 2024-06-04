package org.sol4k.api

data class Blockhash(
    val blockhash: String,
    val slot: Long,
    val lastValidBlockHeight: Long,
)
