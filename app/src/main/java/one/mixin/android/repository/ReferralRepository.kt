package one.mixin.android.repository

import javax.inject.Inject
import one.mixin.android.api.referral.ReferralShareInfo
import one.mixin.android.api.referral.calculateReferralRebatePercentOrNull
import one.mixin.android.api.referral.requestReferralMixinAPI
import one.mixin.android.api.response.WalletHomeBanner
import one.mixin.android.api.response.referral.ReferralCode
import one.mixin.android.api.response.referral.ReferralResponse
import one.mixin.android.api.service.ReferralService
import one.mixin.android.Constants.RouteConfig.REFERRAL_BOT_USER_ID
import one.mixin.android.session.Session
import one.mixin.android.vo.Membership
import timber.log.Timber

class ReferralRepository
    @Inject
    constructor(
        private val referralService: ReferralService,
        private val userRepository: UserRepository,
    ) {
        suspend fun fetchDefaultReferralShareInfoOrNull(logLabel: String): ReferralShareInfo? {
            if (!hasValidReferralMembership(Session.getAccount()?.membership)) {
                Timber.d("Skip fetch referral before %s because membership is invalid or expired", logLabel)
                return null
            }

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

        suspend fun fetchHasBeenInvitedOrNull(logLabel: String): Boolean? {
            if (!hasValidReferralMembership(Session.getAccount()?.membership)) {
                Timber.d("Skip fetch referral before %s because membership is invalid or expired", logLabel)
                return null
            }

            runCatching {
                userRepository.getBotPublicKey(REFERRAL_BOT_USER_ID, false)
            }.onFailure {
                Timber.w(it, "Failed to warm up referral bot session before %s", logLabel)
            }

            return requestReferralMixinAPI(
                invokeNetwork = { referralService.referral() },
                successBlock = { response -> response.data?.hasBeenInvited },
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

        suspend fun fetchWalletHomeBanners(chains: List<String> = emptyList()): List<WalletHomeBanner> {
            runCatching {
                userRepository.getBotPublicKey(REFERRAL_BOT_USER_ID, false)
            }.onFailure {
                Timber.w(it, "Failed to warm up referral bot session before fetching wallet home banners")
            }

            return requestReferralMixinAPI(
                invokeNetwork = { referralService.walletHomeBanners(chains.takeIf { it.isNotEmpty() }) },
                successBlock = { response -> response.data.orEmpty() },
                failureBlock = { response ->
                    Timber.d(
                        "Fetch wallet home banners failed code=%s message=%s",
                        response.errorCode,
                        response.errorDescription,
                    )
                    true
                },
                exceptionBlock = {
                    Timber.w(it, "Fetch wallet home banners failed")
                    true
                },
                requestSession = { userRepository.fetchSessionsSuspend(it) },
            ).orEmpty()
        }

    }

internal fun hasValidReferralMembership(membership: Membership?): Boolean = membership?.isMembership() == true

private fun ReferralCode.toReferralShareInfo(
    referralResponse: ReferralResponse,
): ReferralShareInfo {
    return ReferralShareInfo(
        code = code,
        rebatePercent = calculateReferralRebatePercentOrNull(
            tradingCommissionRatio = referralResponse.tradingCommissionRatio,
            inviterPercent = inviterPercent,
        ),
    )
}
