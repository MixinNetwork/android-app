package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName

data class ParseTxRequest(
    @SerializedName("raw_transaction")
    val rawTransaction: String,
    @SerializedName("chain_id")
    val chainId: String,
    @SerializedName("from")
    val from: String? = null,
)