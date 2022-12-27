package one.mixin.android.pay

import one.mixin.android.Constants
import one.mixin.android.api.response.AddressFeeResponse
import one.mixin.android.pay.erc681.parseERC681
import timber.log.Timber
import java.math.BigDecimal

data class EthereumURI(val uri: String)

internal suspend fun parseEthereum(
    url: String,
    getAddressFee: suspend (String, String) -> AddressFeeResponse?,
): ExternalTransfer? {
    val erc681 = parseERC681(url)

    Timber.d("@@@ $erc681")
    if (!erc681.valid) return null
    val chainId = erc681.chainId?.toInt() ?: 1
    val assetId = ethereumChainIdMap[chainId] ?: return null

    var value = erc681.value
    var address: String? = null
    if (value == null) {
        if (erc681.function != "transfer") return null

        val fp = erc681.functionParams
        var amountExists = false
        // value > amount > uint256
        fp.forEach { pair ->
            if (pair.first == "address") {
                address = pair.second
            } else if (value == null) {
                if (pair.first == "amount") {
                    value = pair.second.toBigIntegerOrNull()
                    amountExists = true
                } else if (!amountExists || pair.first == "uint256") {
                    value = pair.second.toBigIntegerOrNull()
                }
            }
        }
    } else {
        address = erc681.address
    }
    val destination = address ?: return null

    val amount = Convert.fromWei(BigDecimal(value), Convert.Unit.ETHER)
    val addressFeeResponse = getAddressFee(assetId, destination) ?: return null

    return ExternalTransfer(destination, amount, assetId, addressFeeResponse.fee.toBigDecimalOrNull())
}

private val ethereumChainIdMap by lazy {
    mapOf(
        1 to Constants.ChainId.ETHEREUM_CHAIN_ID,
    )
}
