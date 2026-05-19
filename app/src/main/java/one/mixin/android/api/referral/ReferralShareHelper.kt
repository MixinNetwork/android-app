package one.mixin.android.api.referral

import one.mixin.android.Constants.Scheme.HTTPS_REFERRALS
import java.io.Serializable
import java.math.BigDecimal

data class ReferralShareInfo(
    val code: String,
    val rebatePercent: String?,
) : Serializable

fun buildReferralShareUrl(referralCode: String): String = "$HTTPS_REFERRALS/$referralCode"

fun buildReferralCopyUrl(
    referralCode: String?,
    defaultUrl: String,
    legacyReferralUrl: String? = null,
): String = referralCode?.let(::buildReferralShareUrl) ?: legacyReferralUrl ?: defaultUrl

internal fun calculateReferralRebatePercentOrNull(
    tradingCommissionRatio: String?,
    inviterPercent: String?,
): String? {
    val tradingRatio = tradingCommissionRatio?.trim()?.toBigDecimalOrNull()
    val inviterRatio = inviterPercent?.trim()?.toBigDecimalOrNull()
    if (tradingRatio == null || inviterRatio == null) return null

    // The displayed invitee rebate is half of the trading commission ratio.
    // inviterPercent is still parsed so malformed code-level data does not produce a misleading display.
    val percent = tradingRatio
        .multiply(HALF)
        .multiply(HUNDRED)
        .stripTrailingZeros()
        .toPlainString()
    return "$percent%"
}

private val HALF = BigDecimal("0.5")
private val HUNDRED = BigDecimal("100")
