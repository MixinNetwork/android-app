package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class WithdrawalResponse(
    @SerializedName("asset_id")
    val assetId: String?,
    val amount: String?,
)
