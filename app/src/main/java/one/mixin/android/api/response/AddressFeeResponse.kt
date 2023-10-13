package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class AddressFeeResponse(
    val destination: String,
    val tag: String? = null,
    @SerializedName("fee_asset_id")
    val assetId: String,
    val fee: String,
)
