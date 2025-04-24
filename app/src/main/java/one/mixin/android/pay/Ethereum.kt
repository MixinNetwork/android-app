package one.mixin.android.pay

import one.mixin.android.Constants
import one.mixin.android.api.response.AddressResponse
import one.mixin.android.api.response.WithdrawalResponse
import one.mixin.android.extension.stripAmountZero
import one.mixin.android.pay.erc681.scientificNumberRegEx
import one.mixin.android.pay.erc681.toERC681
import one.mixin.android.vo.AssetPrecision
import org.web3j.utils.Convert
import java.math.BigDecimal

data class EthereumURI(val uri: String)

internal suspend fun parseEthereum(
    url: String,
    validateAddress: suspend (String, String, String) -> AddressResponse?,
    getFee: suspend (String, String) -> List<WithdrawalResponse>?,
    findAssetIdByAssetKey: suspend (String) -> String?,
    getAssetPrecisionById: suspend (String) -> AssetPrecision?,
    balanceCheck: suspend (String, BigDecimal, String?, BigDecimal?) -> Unit,
): ExternalTransfer? {
    val erc681 = EthereumURI(url).toERC681()
    if (!erc681.valid) return null

    val chainId = erc681.chainId?.toInt() ?: 1
    val chain = ethereumChainIdMap[chainId] ?: return null
    var assetId = chain

    val value = erc681.value
    var address: String? = null
    val amountTmp: BigDecimal? = erc681.amount
    var uint256Tmp: BigDecimal? = null
    var valueTmp: BigDecimal? = null

    if (value != null) {
        address = erc681.address
        valueTmp = Convert.fromWei(BigDecimal(value), Convert.Unit.ETHER)
    }

    val reqAsset = erc681.functionParams.find { pair -> pair.first == "req-asset" }

    if (reqAsset != null) {
        val assetKey = reqAsset.second.lowercase()
        assetId = findAssetIdByAssetKey(assetKey) ?: return null
        address = erc681.address
    } else if (erc681.function != "transfer") {
        address = erc681.address
    } else {
        val assetKey = erc681.address?.lowercase() ?: return null
        assetId = findAssetIdByAssetKey(assetKey) ?: return null

        val fp = erc681.functionParams
        run loop@{
            fp.forEach { pair ->
                if (pair.first == "address") {
                    address = pair.second
                } else if (pair.first == "uint256") {
                    uint256Tmp = pair.second.uint256ToBigDecimal()
                    if (uint256Tmp?.compareTo(BigDecimal(pair.second)) != 0) {
                        return null
                    }
                    val assetPrecision = getAssetPrecisionById(assetId) ?: return null
                    uint256Tmp = uint256Tmp?.divide(BigDecimal.TEN.pow(assetPrecision.precision))
                }
            }
        }
    }

    val destination = address ?: return null
    var amount: BigDecimal? = null
    if (valueTmp != null) {
        amount = valueTmp
    }
    if (amountTmp != null) {
        if (amount == null) {
            if (amountTmp.toPlainString().amountWithE()) {
                return null
            }
            amount = amountTmp
        } else if (amount.compareTo(amountTmp) != 0) {
            return null
        }
    }
    if (uint256Tmp != null) {
        if (amount == null) {
            amount = uint256Tmp
        } else if (amount.compareTo(uint256Tmp) != 0) {
            return null
        }
    }

    val addressResponse = validateAddress(assetId, chain, destination) ?: return null
    if (!addressResponse.destination.equals(destination, true)) {
        return null
    }
    
    val amountStr = amount?.toPlainString()?.stripAmountZero()
    if (amountStr.isNullOrEmpty() || amountStr == "0") {
        return ExternalTransfer(addressResponse.destination, null, assetId, null, null)
    }
    
    val feeResponse = getFee(assetId, destination) ?: return null
    val fee = feeResponse.firstOrNull() ?: return null
    if (fee.assetId == assetId) {
        val totalAmount = amountStr.toBigDecimal() + (fee.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
        balanceCheck(assetId, totalAmount, null, null)
    } else {
        balanceCheck(assetId, amountStr.toBigDecimal(), fee.assetId, fee.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
    }

    return ExternalTransfer(addressResponse.destination, amountStr, assetId, fee.amount?.toBigDecimalOrNull(), fee.assetId)
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
        56 to Constants.ChainId.BinanceSmartChain,
        137 to Constants.ChainId.Polygon,
        8453 to Constants.ChainId.Base,
    )
}
