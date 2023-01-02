package one.mixin.android.pay

import one.mixin.android.Constants
import one.mixin.android.api.response.AddressFeeResponse
import one.mixin.android.extension.stripAmountZero
import one.mixin.android.extension.toUri
import one.mixin.android.pay.erc831.isEthereumURLString
import timber.log.Timber

suspend fun parseExternalTransferUri(
    url: String,
    getAddressFee: suspend (String, String) -> AddressFeeResponse?,
    findAssetIdByAssetKey: suspend (String) -> String?,
): ExternalTransfer? {
    Timber.d("parseExternalTransferUri: $url")
    if (url.isEthereumURLString()) {
        return parseEthereum(url, getAddressFee, findAssetIdByAssetKey)
    }

    val uri = url.replaceFirst(":", "://").toUri()
    val scheme = uri.scheme
    val assetId = externalTransferAssetIdMap[scheme] ?: return null

    if (assetId == Constants.ChainId.Solana) {
        val splToken = uri.getQueryParameter("spl-token")
        if (splToken != null) {
            return null
        }
    }

    val destination = uri.host ?: return null
    val addressFeeResponse = getAddressFee(assetId, destination) ?: return null

    var amount = uri.getQueryParameter("amount")?.stripAmountZero()
    if (amount == null) {
        amount = uri.getQueryParameter("tx_amount")?.stripAmountZero()
    }
    if (amount == null) return null
    val amountBD = amount.toBigDecimalOrNull() ?: return null

    return ExternalTransfer(addressFeeResponse.destination, amountBD, assetId, addressFeeResponse.fee.toBigDecimalOrNull())
}

val externalTransferAssetIdMap by lazy {
    mapOf(
        "bitcoin" to Constants.ChainId.BITCOIN_CHAIN_ID,
        "ethereum" to Constants.ChainId.ETHEREUM_CHAIN_ID,
        "litecoin" to Constants.ChainId.Litecoin,
        "dash" to Constants.ChainId.Dash,
        "dogecoin" to Constants.ChainId.Dogecoin,
        "monero" to Constants.ChainId.Monero,
        "solana" to Constants.ChainId.Solana,
    )
}
