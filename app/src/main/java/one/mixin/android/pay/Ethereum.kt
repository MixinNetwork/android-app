package one.mixin.android.pay

import one.mixin.android.Constants
import one.mixin.android.api.response.AddressFeeResponse
import one.mixin.android.pay.erc681.parseERC681
import one.mixin.android.pay.erc681.scientificNumberRegEx
import timber.log.Timber
import java.math.BigDecimal

data class EthereumURI(val uri: String)

internal suspend fun parseEthereum(
    url: String,
    getAddressFee: suspend (String, String) -> AddressFeeResponse?,
    findAssetIdByAssetKey: suspend (String) -> String?,
): ExternalTransfer? {
    val erc681 = parseERC681(url)
    Timber.d("parseEthereum: $erc681")

    // if (!erc681.valid) return null

    val chainId = erc681.chainId?.toInt() ?: 1
    var assetId = ethereumChainIdMap[chainId] ?: return null

    val value = erc681.value
    var address: String? = null
    var amount: BigDecimal? = null
    if (value == null) {
        if (erc681.function != "transfer") return null

        val assetKey = erc681.address?.lowercase() ?: return null
        assetId = findAssetIdByAssetKey(assetKey) ?: return null

        val fp = erc681.functionParams
        fp.forEach { pair ->
            if (pair.first == "address") {
                address = pair.second
            } else {
                if (pair.first == "amount") {
                    amount = BigDecimal(pair.second)
                } else if (pair.first == "uint256") {
                    amount = pair.second.dropE2BigDecimal()
                }
            }
        }
    } else {
        address = erc681.address
        amount = Convert.fromWei(BigDecimal(value), Convert.Unit.ETHER)
    }
    val destination = address ?: return null
    val amountBD = amount ?: return null

    val addressFeeResponse = getAddressFee(assetId, destination) ?: return null

    return ExternalTransfer(addressFeeResponse.destination, amountBD, assetId, addressFeeResponse.fee.toBigDecimalOrNull())
}

fun String?.dropE2BigDecimal(): BigDecimal? {
    if (this == null) {
        return null
    }

    if (!scientificNumberRegEx.matches(this)) {
        return null
    }

    return when {
        contains("e") -> {
            val split = split("e")
            BigDecimal(split.first())
        }
        else -> BigDecimal(this)
    }
}

private val ethereumChainIdMap by lazy {
    mapOf(
        1 to Constants.ChainId.ETHEREUM_CHAIN_ID,
    )
}
