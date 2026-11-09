package one.mixin.android.vo.sumsub

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    @SerializedName("asset_ids")
    val assetIds: ArrayList<String>,
    @SerializedName("currencies")
    val currencies: ArrayList<String>,
)
