package one.mixin.android.api.response.web3

import com.google.gson.annotations.SerializedName

data class StakeResponse(
    @SerializedName("tx")
    val tx: String,
)