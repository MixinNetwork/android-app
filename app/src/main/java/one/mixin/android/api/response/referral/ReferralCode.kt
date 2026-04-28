package one.mixin.android.api.response.referral

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ReferralCode(
    @SerializedName("code") val code: String,
    @SerializedName("description") val description: String,
    @SerializedName("inviter_percent") val inviterPercent: String?,
    @SerializedName("is_default") val isDefault: Boolean,
    @SerializedName("total_referrals") val totalReferrals: Int,
    @SerializedName("traded_referrals") val tradedReferrals: Int,
    @SerializedName("total_commissions") val totalCommissions: String,
) : Serializable
