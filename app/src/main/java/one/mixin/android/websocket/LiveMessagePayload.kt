package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName

data class LiveMessagePayload(
    @SerializedName("width")
    val width: Int,
    @SerializedName("height")
    val height: Int,
    @SerializedName("thumb_url")
    val thumbUrl: String,
    @SerializedName("url")
    val url: String,
    val shareable: Boolean?
)
