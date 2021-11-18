package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class BlazeAckMessage(
    @SerializedName("message_id")
    val message_id: String,
    @SerializedName("status")
    val status: String
) : Serializable
