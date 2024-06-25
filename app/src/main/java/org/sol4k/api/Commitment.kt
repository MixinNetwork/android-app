package org.sol4k.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Commitment {
    @SerialName("finalized")
    FINALIZED,

    @SerialName("confirmed")
    CONFIRMED,

    @SerialName("processed")
    PROCESSED;

    override fun toString(): String {
        return this.name.toLowerCase()
    }
}
