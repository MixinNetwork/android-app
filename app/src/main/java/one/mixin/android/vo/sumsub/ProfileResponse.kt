package one.mixin.android.vo.sumsub

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    @SerializedName("buy_enable")
    val buyEnable: Boolean,
    @SerializedName("kyc_enable")
    val kycEnable: Boolean,
    @SerializedName("asset_ids")
    val assetIds: ArrayList<String>,
    val currencies: ArrayList<String>,
    @SerializedName("support_payments")
    val supportPayments: ArrayList<String>,
)
