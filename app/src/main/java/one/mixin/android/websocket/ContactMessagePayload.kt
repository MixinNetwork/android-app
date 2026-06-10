package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName

data class ContactMessagePayload(
    @SerializedName("user_id")
    val userId: String,
)
