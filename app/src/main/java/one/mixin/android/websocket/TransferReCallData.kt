package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName

data class TransferReCallData(
    @SerializedName("message_id")
    val messageId: String
)