package one.mixin.android.api.response.referral

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ReferralResponse(
    @SerializedName("codes") val codes: List<ReferralCode>,
    @SerializedName("total_referrals") val totalReferrals: Int,
    @SerializedName("traded_referrals") val tradedReferrals: Int,
    @SerializedName("total_commissions") val totalCommissions: String,
    @SerializedName("trading_commission_ratio") val tradingCommissionRatio: String,
    @SerializedName("membership_level") val membershipLevel: String,
    @SerializedName("expired_at") val expiredAt: String?,
    @SerializedName("has_been_invited") val hasBeenInvited: Boolean,
) : Serializable
