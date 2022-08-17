package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName

data class StickerMessagePayload(
    @SerializedName("sticker_id")
    val stickerId: String? = null,
    @SerializedName("name")
    val name: String? = null
)
