package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName

data class Web3RawTransactionRequest(
    @SerializedName("chain_id")
    val chainId: String,
    @SerializedName("raw_transaction")
    val rawTransaction: String,
    @SerializedName("from")
    val from: String?,
    @SerializedName("to")
    val to : String?,
    @SerializedName("fee_type")
    val feeType: String? = null,
)