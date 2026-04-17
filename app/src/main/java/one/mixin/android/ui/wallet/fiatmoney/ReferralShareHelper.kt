package one.mixin.android.ui.wallet.fiatmoney

import one.mixin.android.Constants.RouteConfig.REFERRAL_BOT_USER_ID
import one.mixin.android.Constants.Scheme.HTTPS_REFERRALS
import one.mixin.android.api.response.referral.ReferralCode
import one.mixin.android.api.service.ReferralService
import one.mixin.android.repository.UserRepository
import java.io.Serializable
import timber.log.Timber

data class ReferralShareInfo(
    val code: String,
    val rebatePercent: String,
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
            response.data?.codes
                ?.firstOrNull { it.isDefault }
                ?.toReferralShareInfo()
                ?: response.data?.codes?.firstOrNull()?.toReferralShareInfo()
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

private fun ReferralCode.toReferralShareInfo(): ReferralShareInfo {
    return ReferralShareInfo(
        code = code,
        rebatePercent = inviterPercent.toRebatePercent(),
    )
}

private fun String?.toRebatePercent(): String {
    val normalized = this?.trim().orEmpty()
    return when {
        normalized.isEmpty() -> DEFAULT_REBATE_PERCENT
        normalized.endsWith("%") -> normalized
        else -> "$normalized%"
    }
}

private const val DEFAULT_REBATE_PERCENT = "20%"
