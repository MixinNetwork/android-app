package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName

data class TransferStickerData(
    @SerializedName("sticker_id")
    val stickerId: String? = null,
    @SerializedName("album_id")
    val albumId: String? = null,
    @SerializedName("name")
    val name: String? = null
)