package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class BlazeAckMessage(
    @SerializedName("message_id")
    val messageId: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("expire_at")
    val expireAt: Long? = null
) : Serializable
