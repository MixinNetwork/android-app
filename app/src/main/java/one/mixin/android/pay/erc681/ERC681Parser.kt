package one.mixin.android.pay.erc681

import one.mixin.android.pay.CommonEthereumURIData
import one.mixin.android.pay.EthereumURI
import one.mixin.android.pay.erc831.ERC831
import one.mixin.android.pay.parseCommonURI
import java.math.BigDecimal
import java.math.BigInteger

private val scientificNumberRegEx = Regex("^\\d+(\\.\\d+)?(e\\d+)?$")

fun EthereumURI.toERC681() = parseCommonURI().toERC681()

fun ERC831.toERC681() = parseCommonURI().toERC681()

fun CommonEthereumURIData.toERC681() = let { commonURI ->
    ERC681().apply {
        scheme = commonURI.scheme
        prefix = commonURI.prefix
        chainId = commonURI.chainId
        function = commonURI.function
        address = commonURI.address
        valid = commonURI.valid

        fun String?.toBigInteger(): BigInteger? {
            if (this == null) {
                return null
            }

            if (!scientificNumberRegEx.matches(this)) {
                valid = false
                return null
            }

            return when {
                contains("e") -> {
                    val split = split("e")
                    BigDecimal(split.first()).multiply(BigDecimal.TEN.pow(split[1].toIntOrNull() ?: 1)).toBigInteger()
                }
                contains(".") -> {
                    valid = false
                    null
                }
                else -> BigInteger(this)
            }
        }

        val queryAsMap = commonURI.query.toMap() // should be improved https://github.com/walleth/kethereum/issues/25

        gasLimit = (queryAsMap["gas"] ?: queryAsMap["gasLimit"]).toBigInteger()
        gasPrice = (queryAsMap["gasPrice"]).toBigInteger()
        value = queryAsMap["value"]?.split("-")?.first()?.toBigInteger()

        functionParams = commonURI.query.filter { it.first != "gas" && it.first != "value" }
    }
}

fun parseERC681(url: String) = EthereumURI(url).toERC681()
