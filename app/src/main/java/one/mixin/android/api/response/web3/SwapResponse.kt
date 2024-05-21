package one.mixin.android.api.response.web3

import com.google.gson.annotations.SerializedName

data class SwapResponse(
    @SerializedName("swapTransaction") val swapTransaction: String,
    @SerializedName("lastValidBlockHeight") val lastValidBlockHeight: Int,
    @SerializedName("prioritizationFeeLamports") val prioritizationFeeLamports: Int? = null
)