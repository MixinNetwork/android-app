package one.mixin.android.vo

import com.google.gson.annotations.SerializedName

class ChainDapp(
    @SerializedName("chain_id")
    val chainId: String,
    @SerializedName("rpc_urls")
    val rpcUrls: List<String>,
    val dapps: List<Dapp>,
)
