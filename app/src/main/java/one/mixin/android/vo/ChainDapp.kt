package one.mixin.android.vo

import com.google.gson.annotations.SerializedName

class ChainDapp(
    @SerializedName("chain_id")
    val chainId: String,
    val rpc: String,
    val dapps: List<Dapp>
)
