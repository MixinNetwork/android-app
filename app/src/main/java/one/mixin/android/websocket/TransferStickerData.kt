package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName

data class TransferStickerData(
    @SerializedName("album_id")
    val albumId: String,
    @SerializedName("name")
    val name: String
)