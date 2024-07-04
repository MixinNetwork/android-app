package one.mixin.android.vo

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppCardData(
    @SerializedName("app_id")
    val appId: String?,
    @SerializedName("icon_url")
    val iconUrl: String,
    var title: String,
    var description: String,
    val action: String?,
    @SerializedName("updated_at")
    val updatedAt: String?,
    val shareable: Boolean?,
    val actions: List<Action>? = null,
) : Parcelable {
    init {
        title = title.take(36)
        description = description.take(512)
    }

    @IgnoredOnParcel
    val newVersion :Boolean
        get() {
            return action.isNullOrBlank() && actions?.isNotEmpty() == true
        }
}

@Parcelize
data class Action(
    val label: String,
    val color: String,
    val action: String,
) : Parcelable