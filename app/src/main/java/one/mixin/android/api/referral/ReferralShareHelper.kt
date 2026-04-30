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

    // rebate = trading commission ratio * (1 - inviter ratio), and inviter ratio above 100% is clamped to 0 rebate.
    val percent = tradingRatio
        .multiply((BigDecimal.ONE - inviterRatio).coerceAtLeast(BigDecimal.ZERO))
        .multiply(HUNDRED)
        .stripTrailingZeros()
        .toPlainString()
    return "$percent%"
}

private val HUNDRED = BigDecimal("100")
