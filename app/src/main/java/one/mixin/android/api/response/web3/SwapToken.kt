package one.mixin.android.api.response.web3

import com.google.gson.annotations.SerializedName

data class SwapToken(
    @SerializedName("address") val address: String,
    @SerializedName("chainId") val chainId: Int,
    @SerializedName("decimals") val decimals: Int,
    @SerializedName("name") val name: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("logoURI") val logoURI: String,
    @SerializedName("tags") val tags: List<String>,
)
