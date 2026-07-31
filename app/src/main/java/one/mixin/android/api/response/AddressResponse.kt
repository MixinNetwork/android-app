package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class AddressResponse(
    @SerializedName("destination")
    val destination: String,
    @SerializedName("tag")
    val tag: String? = null,
    @SerializedName("fee_asset_id")
    val assetId: String,
)
