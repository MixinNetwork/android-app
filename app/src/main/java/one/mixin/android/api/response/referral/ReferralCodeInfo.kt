package one.mixin.android.api.response.referral

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ReferralCodeInfo(
    @SerializedName("code") val code: String,
    @SerializedName("invitee_percent") val inviteePercent: String,
    @SerializedName("inviter_user_id") val inviterUserId: String,
) : Serializable
