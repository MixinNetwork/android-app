package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName

data class WalletRequest(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("category")
    val category: String,
    
    @SerializedName("addresses")
    val addresses: List<Web3AddressRequest>?
)
