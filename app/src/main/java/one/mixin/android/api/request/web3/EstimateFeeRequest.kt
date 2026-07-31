package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName

data class EstimateFeeRequest(
    @SerializedName("chain_id")
    val chainId: String,
    @SerializedName("raw_transaction")
    val rawTransaction: String?,
    @SerializedName("data")
    val data: String?,
    @SerializedName("from")
    val from: String? = null,
    @SerializedName("to")
    val to: String? = null,
    @SerializedName("value")
    val value: String? = null,
    @SerializedName("fee_rate")
    val rate: String? = null,
)