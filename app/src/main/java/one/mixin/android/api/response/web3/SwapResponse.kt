package one.mixin.android.api.response.web3

import com.google.gson.annotations.SerializedName

data class SwapResponse(
    @SerializedName("swap_transaction") val swapTransaction: String,
    @SerializedName("last_valid_block_height") val lastValidBlockHeight: Int,
    @SerializedName("prioritization_fee_lamports") val prioritizationFeeLamports: Int? = null
)