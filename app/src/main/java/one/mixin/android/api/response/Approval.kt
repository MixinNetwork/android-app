package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class Approval(
    val name: String,
    val symbol: String,
    @SerializedName("icon_url") val iconUrl: String,
    val sender: String,
    val amount: String
)
