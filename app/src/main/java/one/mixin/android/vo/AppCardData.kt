package one.mixin.android.vo

import com.google.gson.annotations.SerializedName

data class AppCardData(
    @SerializedName("app_id")
    val appId: String?,
    @SerializedName("icon_url")
    val iconUrl: String,
    var title: String,
    var description: String,
    val action: String
) {
    init {
        title = title.take(36)
        description = description.take(128)
    }
}
