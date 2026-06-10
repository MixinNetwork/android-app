package one.mixin.android.pay.erc831

import one.mixin.android.pay.EthereumURI
import one.mixin.android.pay.erc831.ParseState.PAYLOAD
import one.mixin.android.pay.erc831.ParseState.PREFIX
import one.mixin.android.pay.erc831.ParseState.SCHEMA

// as defined in http://eips.ethereum.org/EIPS/eip-831

private enum class ParseState {
    SCHEMA,
    PREFIX,
    PAYLOAD,
}

fun EthereumURI.toERC831() =
    ERC831().apply {
        var currentSegment = ""

        var currentState = SCHEMA

        fun stateTransition(newState: ParseState) {
            when (currentState) {
                SCHEMA -> scheme = currentSegment
                PREFIX -> prefix = currentSegment
                PAYLOAD -> payload = currentSegment
            }
            currentState = newState
            currentSegment = ""
        }

        uri.forEach { char ->
            when {
                char == ':' && currentState == SCHEMA
                -> stateTransition(if (uri.hasPrefix()) PREFIX else PAYLOAD)

                char == '-' && currentState == PREFIX
                -> stateTransition(PAYLOAD)

                else -> currentSegment += char
            }
        }

        if (!currentSegment.isBlank()) {
            stateTransition(PAYLOAD)
        }
    }

private fun String.hasPrefix() = contains('-') && (!contains("0x") || indexOf('-') < indexOf("0x"))

fun parseERC831(url: String) = EthereumURI(url).toERC831()
