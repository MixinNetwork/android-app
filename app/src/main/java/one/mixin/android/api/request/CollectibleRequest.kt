package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class CollectibleRequest(
    @SerializedName("action")
    val action: String,
    @SerializedName("raw")
    val raw: String,
    @SerializedName("pin")
    val pin: String,
)
