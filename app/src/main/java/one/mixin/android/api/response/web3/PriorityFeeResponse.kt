package one.mixin.android.api.response.web3

import com.google.gson.annotations.SerializedName

data class PriorityFeeResponse(
    @SerializedName("priority_fee_estimate")
    val priorityFeeEstimate: Double,
    @SerializedName("priority_fee_levels")
    val priorityFeeLevels: Double,
)
