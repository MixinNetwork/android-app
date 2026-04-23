package one.mixin.android.api.referral

import one.mixin.android.Constants.RouteConfig.REFERRAL_BOT_USER_ID
import one.mixin.android.Constants.Scheme.HTTPS_REFERRALS
import one.mixin.android.api.response.referral.ReferralCode
import one.mixin.android.api.response.referral.ReferralResponse
import one.mixin.android.api.service.ReferralService
import one.mixin.android.repository.UserRepository
import java.io.Serializable
import java.math.BigDecimal
import timber.log.Timber

data class ReferralShareInfo(
    val code: String,
    val rebatePercent: String?,
) : Serializable

fun buildReferralShareUrl(referralCode: String): String = "$HTTPS_REFERRALS/$referralCode"

suspend fun fetchDefaultReferralShareInfoOrNull(
    referralService: ReferralService,
    userRepository: UserRepository,
    logLabel: String,
): ReferralShareInfo? {
    runCatching {
        userRepository.getBotPublicKey(REFERRAL_BOT_USER_ID, false)
    }.onFailure {
        Timber.w(it, "Failed to warm up referral bot session before %s", logLabel)
    }

    return requestReferralMixinAPI(
        invokeNetwork = { referralService.referral() },
        successBlock = { response ->
            response.data?.let { referralResponse ->
                referralResponse.codes
                    .firstOrNull { it.isDefault }
                    ?.toReferralShareInfo(referralResponse)
                    ?: referralResponse.codes.firstOrNull()?.toReferralShareInfo(referralResponse)
            }
        },
        failureBlock = { response ->
            Timber.d(
                "Fetch referral before %s failed code=%s message=%s",
                logLabel,
                response.errorCode,
                response.errorDescription,
            )
            true
        },
        exceptionBlock = {
            Timber.w(it, "Fetch referral before %s failed", logLabel)
            true
        },
        requestSession = { userRepository.fetchSessionsSuspend(it) },
    )
}

private fun ReferralCode.toReferralShareInfo(referralResponse: ReferralResponse): ReferralShareInfo {
    return ReferralShareInfo(
        code = code,
        rebatePercent = calculateRebatePercent(
            tradingCommissionRatio = referralResponse.tradingCommissionRatio,
            inviterPercent = inviterPercent,
        ),
    )
}

private fun calculateRebatePercent(
    tradingCommissionRatio: String?,
    inviterPercent: String?,
): String? {
    val tradingRatio = tradingCommissionRatio?.trim()?.toBigDecimalOrNull()
    val inviterRatio = inviterPercent?.trim()?.toBigDecimalOrNull()
    if (tradingRatio == null || inviterRatio == null) return null

    val percent = tradingRatio
        .multiply((BigDecimal.ONE - inviterRatio).coerceAtLeast(BigDecimal.ZERO))
        .multiply(HUNDRED)
        .stripTrailingZeros()
        .toPlainString()
    return "$percent%"
}

private val HUNDRED = BigDecimal("100")
