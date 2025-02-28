package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName

data class AddressRequest(
    @SerializedName("destination")
    val destination: String,
    
    @SerializedName("tag")
    val tag: String,
    
    @SerializedName("chain_id")
    val chainId: String
)
