package one.mixin.android.vo

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import one.mixin.android.util.MoshiHelper.getTypeAdapter

@Parcelize
@JsonClass(generateAdapter = true)
data class AppCardData(
    @SerializedName("app_id")
    @Json(name = "app_id")
    val appId: String?,
    @SerializedName("icon_url")
    @Json(name = "icon_url")
    val iconUrl: String,
    var title: String,
    var description: String,
    val action: String,
    @SerializedName("updated_at")
    @Json(name = "updated_at")
    val updatedAt: String?,
    val shareable: Boolean?,
) : Parcelable {
    init {
        title = title.take(36)
        description = description.take(128)
    }
}

fun AppCardData.toJson(): String = getTypeAdapter<AppCardData>(AppCardData::class.java).toJson(this)
