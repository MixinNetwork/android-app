package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName

data class EstimateFeeRequest(
    @SerializedName("chain_id")
    val chainId: String,
    @SerializedName("raw_transaction")
    val rawTransaction: String?,

    val from: String? = null,
    val to: String? = null,
)