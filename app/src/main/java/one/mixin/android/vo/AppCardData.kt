package one.mixin.android.vo

import com.google.gson.annotations.SerializedName

data class AppCardData(
    @SerializedName("app_id")
    val appId: String?,
    @SerializedName("icon_url")
    val iconUrl: String,
    val title: String,
    val description: String,
    val action: String
)
