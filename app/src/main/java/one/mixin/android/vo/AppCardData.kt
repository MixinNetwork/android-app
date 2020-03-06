package one.mixin.android.vo

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AppCardData(
    @SerializedName("app_id")
    val appId: String?,
    @SerializedName("icon_url")
    val iconUrl: String,
    var title: String,
    var description: String,
    val action: String,
    @SerializedName("updated_at")
    val updatedAt: String?
) : Parcelable {
    init {
        title = title.take(36)
        description = description.take(128)
    }
}
