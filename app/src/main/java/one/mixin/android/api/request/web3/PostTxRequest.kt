package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName

data class PostTxRequest(
    val raw: String,
    @SerializedName("web3_chain_id")
    val web3ChainId: Int,
)