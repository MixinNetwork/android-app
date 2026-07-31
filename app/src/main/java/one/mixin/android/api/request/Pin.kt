package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class Pin(
    @SerializedName("pin")
    val pin: String,
)
