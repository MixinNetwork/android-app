package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class AppMetadata(
    val name: String,
    @SerializedName("icon_url") val iconUrl: String,
    @SerializedName("contract_address") val contractAddress: String,
    @SerializedName("method_id") val methodId: String,
    @SerializedName("method_name") val methodName: String
)
