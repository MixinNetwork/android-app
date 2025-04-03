package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName

data class AuthRequest(
    @SerializedName("chain_id")
    val chainId: String,
    val address: String,
    val timestamp: String,
    val signature: String,
)