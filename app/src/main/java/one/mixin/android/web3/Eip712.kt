package one.mixin.android.web3

import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.evmChainList
import org.web3j.crypto.StructuredDataEncoder
import java.math.BigInteger

internal fun hashEip712Message(message: String): ByteArray {
    val typedData = JsonParser.parseString(message).asJsonObject
    typedData.keySet().retainAll(setOf("types", "primaryType", "domain", "message"))
    return StructuredDataEncoder(typedData.toString()).hashStructuredData()
}

// The typed data's domain.chainId, or null when it names none.
internal fun eip712ChainId(message: String, errorMessage: String): BigInteger? =
    try {
        val domain = JsonParser.parseString(message).asJsonObject.get("domain")
        val chainId = domain?.asJsonObject?.get("chainId")
        if (chainId == null) null else parseChainId(chainId.asJsonPrimitive)
    } catch (e: RuntimeException) {
        throw IllegalArgumentException(errorMessage, e)
    }

private fun parseChainId(value: JsonPrimitive): BigInteger {
    val actual = if (value.isNumber) {
        value.asBigDecimal.toBigIntegerExact()
    } else {
        require(value.isString)
        val text = value.asString
        if (text.startsWith("0x")) {
            require(text.substring(2).matches(Regex("[0-9a-fA-F]+")))
            BigInteger(text.substring(2), 16)
        } else {
            require(text.matches(Regex("[0-9]+")))
            BigInteger(text)
        }
    }
    require(actual.signum() >= 0)
    return actual
}

// The chain typed data is signed for. The signature binds domain.chainId, so
// it is valid on that chain only, whatever chain the dapp is on: any
// supported EVM chain is accepted, and confirmations show it, so the user sees
// the chain they sign for. Without a chainId: `current`.
internal fun eip712SigningChain(message: String, current: Chain?, errorMessage: String): Chain? {
    val chainId = eip712ChainId(message, errorMessage) ?: return current
    return evmChainList.firstOrNull { BigInteger(it.chainReference) == chainId }
        ?: throw IllegalArgumentException(errorMessage)
}
