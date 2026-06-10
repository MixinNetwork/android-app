package one.mixin.android.vo.sumsub

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    @SerializedName("kyc_state")
    val kycState: String,
    @SerializedName("asset_ids")
    val assetIds: ArrayList<String>,
    val currencies: ArrayList<String>,
    @SerializedName("support_payments")
    val supportPayments: ArrayList<String>,
)
