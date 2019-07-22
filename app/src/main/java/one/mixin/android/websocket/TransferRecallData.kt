package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName

data class TransferRecallData(
    @SerializedName("message_id")
    val messageId: String
)
