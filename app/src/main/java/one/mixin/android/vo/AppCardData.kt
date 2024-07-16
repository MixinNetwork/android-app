package one.mixin.android.vo

import android.net.Uri
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import one.mixin.android.Constants.Scheme.HTTPS_SEND
import one.mixin.android.Constants.Scheme.MIXIN_SEND
import one.mixin.android.Constants.Scheme.SEND

@Parcelize
data class AppCardData(
    @SerializedName("app_id")
    val appId: String?,
    @SerializedName("icon_url")
    val iconUrl: String?,
    @SerializedName("cover_url")
    val coverUrl: String?,
    var title: String?,
    var description: String?,
    val action: String?,
    @SerializedName("updated_at")
    val updatedAt: String?,
    val shareable: Boolean?,
    val actions: List<ActionButtonData>? = null,
) : Parcelable {
    init {
        title = title?.take(36)
        description = description?.take(512)
    }

    @IgnoredOnParcel
    val oldVersion: Boolean
        get() {
            return !action.isNullOrBlank()
        }

    val canShare: Boolean
        get() {
            return if (oldVersion) shareable ?: false
            else {
                shareable == true && (actions.isNullOrEmpty() || actions.all { button ->
                    button.action.isValidShareUrl()
                })
            }
        }
}

private fun String.isValidShareUrl(): Boolean {
    return isValidSendUrl() || (startsWith("HTTPS://", true) || startsWith("HTTP://", true)) && !startsWith(HTTPS_SEND, true)
}

private fun String.isValidSendUrl(): Boolean {
    return (startsWith(SEND, true) || startsWith(MIXIN_SEND, true) || startsWith(HTTPS_SEND, true)) && runCatching {
        Uri.parse(this).getQueryParameter("user").isNullOrEmpty().not()
    }.getOrElse { false }
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
            return action.startsWith(SEND, true) || action.startsWith(MIXIN_SEND, true) || action.startsWith(HTTPS_SEND, true)
        }
}