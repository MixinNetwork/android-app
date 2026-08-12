package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName

data class RpcRequest(
    @SerializedName("method")
    val method: String,
    @SerializedName("params")
    val params: List<Any>,
)