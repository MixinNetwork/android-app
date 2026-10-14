package one.mixin.android.web3

import com.google.gson.JsonParser
import org.web3j.crypto.StructuredDataEncoder

internal fun hashEip712Message(message: String): ByteArray {
    val typedData = JsonParser.parseString(message).asJsonObject
    typedData.keySet().retainAll(setOf("types", "primaryType", "domain", "message"))
    return StructuredDataEncoder(typedData.toString()).hashStructuredData()
}
