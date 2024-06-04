package org.sol4k.rpc

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class SimulateTransactionResponse(val value: SimulateTransactionValue)

@Serializable
internal data class SimulateTransactionValue(
    val err: JsonElement?,
    val logs: List<String>?,
    val unitsConsumed: Long?,
)
