package one.mixin.android.pay

import one.mixin.android.Constants
import one.mixin.android.api.response.AddressFeeResponse
import one.mixin.android.extension.stripAmountZero
import one.mixin.android.pay.erc681.scientificNumberRegEx
import one.mixin.android.pay.erc681.toERC681
import one.mixin.android.vo.AssetPrecision
import java.math.BigDecimal

data class EthereumURI(val uri: String)

internal suspend fun parseEthereum(
    url: String,
    getAddressFee: suspend (String, String) -> AddressFeeResponse?,
    findAssetIdByAssetKey: suspend (String) -> String?,
    getAssetPrecisionById: suspend (String) -> AssetPrecision?,
): ExternalTransfer? {
    val erc681 = EthereumURI(url).toERC681()
    if (!erc681.valid) return null

    val chainId = erc681.chainId?.toInt() ?: 1
    var assetId = ethereumChainIdMap[chainId] ?: return null

    val value = erc681.value
    var address: String? = null
    var amount: BigDecimal? = null
    if (value == null) {
        var needCheckPrecision = false
        if (erc681.function != "transfer") return null

        val assetKey = erc681.address?.lowercase() ?: return null
        assetId = findAssetIdByAssetKey(assetKey) ?: return null

        val fp = erc681.functionParams
        var amountFound = false
        var addressFound = false
        run loop@{
            fp.forEach { pair ->
                if (amountFound && addressFound) {
                    return@loop
                }

                if (pair.first == "address") {
                    address = pair.second
                    addressFound = true
                } else {
                    if (pair.first == "amount") {
                        if (pair.second.amountWithE()) {
                            return null
                        }
                        amount = BigDecimal(pair.second)
                        amountFound = true
                        needCheckPrecision = false
                    } else if (!amountFound && pair.first == "uint256") {
                        amount = pair.second.uint256ToBigDecimal()
                        if (amount?.compareTo(BigDecimal(pair.second)) != 0) {
                            return null
                        }
                        needCheckPrecision = true
                    }
                }
            }
        }
        if (needCheckPrecision) {
            val assetPrecision = getAssetPrecisionById(assetId) ?: return null
            amount = amount?.divide(BigDecimal.TEN.pow(assetPrecision.precision))
        }
    } else {
        address = erc681.address
        amount = Convert.fromWei(BigDecimal(value), Convert.Unit.ETHER)
    }
    val destination = address ?: return null
    val am = amount?.toPlainString()?.stripAmountZero() ?: return null
    val addressFeeResponse = getAddressFee(assetId, destination) ?: return null
    if (!addressFeeResponse.destination.equals(destination, true)) {
        return null
    }
    return ExternalTransfer(addressFeeResponse.destination, am, assetId, addressFeeResponse.fee.toBigDecimalOrNull())
}

fun String?.uint256ToBigDecimal(): BigDecimal? {
    if (this == null) {
        return null
    }

    if (!scientificNumberRegEx.matches(this)) {
        return null
    }

    return when {
        contains("e") -> {
            val split = split("e")
            BigDecimal(split.first()).multiply(BigDecimal.TEN.pow(split[1].toIntOrNull() ?: 1))
        }
        contains(".") -> {
            null
        }
        else -> BigDecimal(this)
    }
}

private val ethereumChainIdMap by lazy {
    mapOf(
        1 to Constants.ChainId.ETHEREUM_CHAIN_ID,
    )
}
