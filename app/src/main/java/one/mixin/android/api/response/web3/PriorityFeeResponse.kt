package one.mixin.android.api.response.web3

import com.google.gson.annotations.SerializedName

data class PriorityFeeResponse(
    @SerializedName("unit_price")
    val unitPrice: Long,
    @SerializedName("unit_limit")
    val unitLimit: Int,
)
