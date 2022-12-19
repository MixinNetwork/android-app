package one.mixin.android.util

import android.net.Uri
import one.mixin.android.Constants
import one.mixin.android.api.response.AddressFeeResponse
import one.mixin.android.extension.stripAmountZero
import java.math.BigDecimal

data class ExternalTransfer (
    val destination: String,
    val amount: BigDecimal,
    val assetId: String,
    val fee: BigDecimal?,
)

suspend fun parseExternalTransferUri(
    uri: Uri,
    getAddressFee: suspend (String, String) -> AddressFeeResponse?,
): ExternalTransfer? {
    val scheme = uri.scheme
    val assetId = externalTransferAssetIdMap[scheme] ?: return null

    if (scheme == "ethereum") {
        return parseEthereum(uri, assetId, getAddressFee)
    }

    val destination = uri.host ?: return null
    val addressFeeResponse = getAddressFee(assetId, destination) ?: return null

    var amount = uri.getQueryParameter("amount")?.stripAmountZero()
    if (amount == null) {
        amount = uri.getQueryParameter("tx_amount")?.stripAmountZero()
    }
    if (amount == null) return null
    val amountBD = amount.toBigDecimalOrNull() ?: return null

    return ExternalTransfer(destination, amountBD, assetId, addressFeeResponse.fee.toBigDecimalOrNull())
}

private suspend fun parseEthereum(
    uri: Uri,
    assetId: String,
    getAddressFee: suspend (String, String) -> AddressFeeResponse?,
): ExternalTransfer? {
    if (uri.scheme != "ethereum") return null

    val pathSegments = uri.pathSegments
    if (pathSegments.size < 1 || pathSegments[0] != "transfer") return null

    val destination = uri.getQueryParameter("address") ?: return null
    val addressFeeResponse = getAddressFee(assetId, destination) ?: return null

    // TODO get amount
    return ExternalTransfer(destination, BigDecimal.ZERO, assetId, addressFeeResponse.fee.toBigDecimalOrNull())
}

val externalTransferAssetIdMap by lazy {
    mapOf(
        "bitcoin" to Constants.ChainId.BITCOIN_CHAIN_ID,
        "ethereum" to Constants.ChainId.ETHEREUM_CHAIN_ID,
        "tron" to Constants.ChainId.TRON_CHAIN_ID,
        "litecoin" to Constants.ChainId.Litecoin,
        "dash" to Constants.ChainId.Dash,
        "dogcoin" to Constants.ChainId.Dogecoin,
        "monero" to Constants.ChainId.Monero,
    )
}