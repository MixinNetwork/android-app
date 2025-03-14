package one.mixin.android.pay

import one.mixin.android.Constants
import one.mixin.android.api.response.AddressResponse
import one.mixin.android.api.response.PaymentResponse
import one.mixin.android.api.response.WithdrawalResponse
import one.mixin.android.extension.stripAmountZero
import one.mixin.android.vo.AssetPrecision
import timber.log.Timber
import java.math.BigDecimal

internal suspend fun parseLightning(
    url: String,
    validateAddress: suspend (String, String) -> AddressResponse?,
    getFee: suspend (String, String) -> List<WithdrawalResponse>?,
    balanceCheck: suspend (String, BigDecimal, String?, BigDecimal?) -> Unit,
    parseLighting: suspend (String) -> PaymentResponse?
): ExternalTransfer? {
    val r = parseLighting(url) ?: return null
    val assetId = r.asset?.assetId ?:return null
    val am = r.amount ?: return null
    if ((am.toBigDecimalOrNull() ?: BigDecimal.ZERO) <= BigDecimal.ZERO) return null
    val destination = r.destination ?: return null
    val addressResponse = validateAddress(assetId, destination) ?: return null
    val feeResponse = getFee(assetId, destination) ?: return null
    val fee = feeResponse.firstOrNull() ?: return null
    if (fee.assetId == assetId) {
        val totalAmount = am.toBigDecimal() + (fee.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
        balanceCheck(assetId, totalAmount, null, null)
    } else {
        balanceCheck(
            assetId,
            am.toBigDecimal(),
            fee.assetId,
            fee.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        )
    }

    return ExternalTransfer(
        addressResponse.destination,
        am,
        assetId,
        fee.amount?.toBigDecimalOrNull(),
        fee.assetId
    )
}
