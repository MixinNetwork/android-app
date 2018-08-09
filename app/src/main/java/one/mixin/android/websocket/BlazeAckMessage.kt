package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class BlazeAckMessage(
    @SerializedName("message_id")
    val message_id: String,
    @SerializedName("status")
    val status: String
) : Serializable
