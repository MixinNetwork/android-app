package org.sol4k.api

import kotlinx.serialization.Serializable

@Serializable
internal data class IsBlockhashValidResult(val value: Boolean)
