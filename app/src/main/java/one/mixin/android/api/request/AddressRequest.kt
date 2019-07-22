package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class AddressRequest(
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("public_key")
    val publicKey: String?,
    val label: String?,
    val pin: String,
    @SerializedName("account_name")
    val accountName: String?,
    @SerializedName("account_tag")
    val accountTag: String?
)
