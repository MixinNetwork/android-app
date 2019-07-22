package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName

data class TransferContactData(
    @SerializedName("user_id")
    val userId: String
)
