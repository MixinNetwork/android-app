package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class StickerAddRequest(
    @SerializedName("data_base64")
    val dataBase64: String? = null,
    @SerializedName("sticker_id")
    val stickerId: String? = null,
)
