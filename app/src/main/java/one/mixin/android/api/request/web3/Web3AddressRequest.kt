package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName

data class Web3AddressRequest(
    @SerializedName("destination")
    val destination: String,
    
    @SerializedName("chain_id")
    val chainId: String,

    @SerializedName("path")
    val path: String?,

    @SerializedName("signature")
    val signature: String? = null,

    @SerializedName("timestamp")
    val timestamp: String? = null
)
