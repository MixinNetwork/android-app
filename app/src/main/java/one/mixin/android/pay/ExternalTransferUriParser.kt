package one.mixin.android.pay

import android.net.Uri
import one.mixin.android.Constants
import one.mixin.android.api.response.AddressResponse
import one.mixin.android.api.response.PaymentResponse
import one.mixin.android.api.response.WithdrawalResponse
import one.mixin.android.extension.isLightningUrl
import one.mixin.android.extension.stripAmountZero
import one.mixin.android.extension.toUri
import one.mixin.android.pay.erc831.isEthereumURLString
import one.mixin.android.vo.AssetPrecision
import org.sol4k.Base58
import timber.log.Timber
import java.math.BigDecimal

suspend fun parseExternalTransferUri(
    url: String,
    validateAddress: suspend (String, String) -> AddressResponse?,
    getFee: suspend (String, String) -> List<WithdrawalResponse>?,
    findAssetIdByAssetKey: suspend (String) -> String?,
    getAssetPrecisionById: suspend (String) -> AssetPrecision?,
    balanceCheck: suspend (String, BigDecimal, String?, BigDecimal?) -> Unit,
    parseLighting: suspend (String) -> PaymentResponse?
): ExternalTransfer? {
    if (url.isEthereumURLString()) {
        return parseEthereum(url, validateAddress, getFee, findAssetIdByAssetKey, getAssetPrecisionById, balanceCheck)
    }

    if (url.isLightningUrl()) {
        return parseLightning(url, validateAddress, getFee, balanceCheck, parseLighting)
    }

    val uri = url.addSlashesIfNeeded().toUri()
    val scheme = uri.scheme?.lowercase()
    val chainId = externalTransferAssetIdMap[scheme] ?: return null

    val splAssetId = if (chainId == Constants.ChainId.Solana) {
        val splToken = uri.getQueryParameter("spl-token")
        if (!splToken.isNullOrEmpty()) {
            try {
                Base58.decode(splToken)
                findAssetIdByAssetKey(splToken)
            } catch (e: Exception) {
                return null
            }
        } else {
            null
        }
    } else null
    val assetId = splAssetId ?: chainId

    val destination = uri.host ?: return null
    val addressResponse = validateAddress(assetId, destination) ?: return null
    if (!addressResponse.destination.equals(destination, true)) {
        return null
    }

    var amount = uri.getQueryParameter("amount")
    if (amount == null) {
        amount = uri.getQueryParameter("tx_amount")
    }
    
    if (amount.isNullOrEmpty() || amount == "0") {
        return ExternalTransfer(addressResponse.destination, null, assetId, null, null, uri.getQueryParameter("memo")?.run {
            Uri.decode(this)
        })
    }
    
    if (amount.amountWithE()) return null

    amount = amount.stripAmountZero()
    val amountBD = amount.toBigDecimalOrNull() ?: return null
    if (amount != amountBD.toPlainString()) {
        return null
    }
    
    val feeResponse = getFee(assetId, destination) ?: return null
    val fee = feeResponse.firstOrNull() ?: return null
    
    if (fee.assetId == assetId) {
        val totalAmount = amountBD + (fee.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
        balanceCheck(assetId, totalAmount, null, null)
    } else {
        balanceCheck(assetId, amountBD, fee.assetId, fee.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
    }

    val memo =
        uri.getQueryParameter("memo")?.run {
            Uri.decode(this)
        }
    return ExternalTransfer(addressResponse.destination, amount, assetId, fee.amount?.toBigDecimalOrNull(), fee.assetId, memo)
}

// check amount has scientific E
fun String?.amountWithE(): Boolean = this?.contains("e") == true

fun String.addSlashesIfNeeded(): String {
    if (this.indexOf("://") != -1) {
        return this
    }
    return this.replace(":", "://")
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
