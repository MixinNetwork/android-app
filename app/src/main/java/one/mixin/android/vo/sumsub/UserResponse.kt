package one.mixin.android.vo.sumsub

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("buy_enable")
    val buyEnable: Boolean,
    @SerializedName("kyc_enable")
    val kycEnable: Boolean,
    @SerializedName("asset_ids")
    val assetIds: ArrayList<String>,
    val currency: ArrayList<String>,
    @SerializedName("support_payment")
    val supportPayment: ArrayList<String>,
)
