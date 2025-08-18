package one.mixin.android.api.request.web3

data class RpcRequest(
    val method: String,
    val params: List<Any>,
)