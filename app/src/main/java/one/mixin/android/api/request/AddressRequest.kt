package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class AddressRequest(
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("chain_id")
    val chainId: String,
    @SerializedName("destination")
    val destination: String?,
    @SerializedName("tag")
    val tag: String?,
    val label: String?,
    val pin: String,
)
