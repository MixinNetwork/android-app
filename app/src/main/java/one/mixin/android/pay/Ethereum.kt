package one.mixin.android.pay

import one.mixin.android.Constants
import one.mixin.android.api.response.AddressFeeResponse
import one.mixin.android.pay.erc681.parseERC681
import java.math.BigDecimal

data class EthereumURI(val uri: String)

internal suspend fun parseEthereum(
    url: String,
    getAddressFee: suspend (String, String) -> AddressFeeResponse?,
): ExternalTransfer? {
    val erc681 = parseERC681(url)

    val chainId = erc681.chainId?.toInt() ?: return null
    val assetId = ethereumChainIdMap[chainId] ?: return null

    val destination = erc681.address ?: return null
    val value = erc681.value ?: return null
    val amount = Convert.fromWei(BigDecimal(value), Convert.Unit.ETHER)
    val addressFeeResponse = getAddressFee(assetId, destination) ?: return null

    return ExternalTransfer(destination, amount, assetId, addressFeeResponse.fee.toBigDecimalOrNull())
}

private val ethereumChainIdMap by lazy {
    mapOf(
        1 to Constants.ChainId.ETHEREUM_CHAIN_ID,
    )
}
