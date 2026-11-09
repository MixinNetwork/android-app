package one.mixin.android.web3

import com.google.gson.JsonParser
import org.web3j.crypto.StructuredDataEncoder
import java.math.BigInteger

internal fun hashEip712Message(message: String): ByteArray {
    val typedData = JsonParser.parseString(message).asJsonObject
    typedData.keySet().retainAll(setOf("types", "primaryType", "domain", "message"))
    return StructuredDataEncoder(typedData.toString()).hashStructuredData()
}

internal fun validateEip712ChainId(message: String, expectedChainReference: String, errorMessage: String) {
    try {
        val domain = JsonParser.parseString(message).asJsonObject.get("domain") ?: return
        val chainId = domain.asJsonObject.get("chainId") ?: return
        val value = chainId.asJsonPrimitive
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
        require(actual.signum() >= 0 && actual == BigInteger(expectedChainReference))
    } catch (e: RuntimeException) {
        throw IllegalArgumentException(errorMessage, e)
    }
}
