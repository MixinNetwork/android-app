package one.mixin.android.vo

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import one.mixin.android.Constants.Scheme.SEND

@Parcelize
data class AppCardData(
    @SerializedName("app_id")
    val appId: String?,
    @SerializedName("icon_url")
    val iconUrl: String,
    @SerializedName("cover_url")
    val coverUrl: String,
    var title: String,
    var description: String,
    val action: String?,
    @SerializedName("updated_at")
    val updatedAt: String?,
    val shareable: Boolean?,
    val actions: List<ActionButtonData>? = null,
) : Parcelable {
    init {
        title = title.take(36)
        description = description.take(512)
    }

    @IgnoredOnParcel
    val oldVersion: Boolean
        get() {
            return !action.isNullOrBlank()
        }
}

@Parcelize
data class ActionButtonData(
    val label: String,
    val color: String,
    val action: String,
) : Parcelable {
    @IgnoredOnParcel
    val externalLink:Boolean
        get() {
            return action.startsWith("http://", true) || action.startsWith("https://")
        }

    @IgnoredOnParcel
    val sendLink:Boolean
        get() {
            return action.startsWith(SEND, true)
        }
}