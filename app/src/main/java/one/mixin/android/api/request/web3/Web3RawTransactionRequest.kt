package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName

data class Web3RawTransactionRequest(
    @SerializedName("chain_id")
    val chainId: String,
    @SerializedName("raw_transaction")
    val rawTransaction: String?
)