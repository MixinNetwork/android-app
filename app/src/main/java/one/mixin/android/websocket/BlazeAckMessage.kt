package one.mixin.android.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class BlazeAckMessage(
    @Json(name = "message_id")
    val message_id: String,
    @Json(name = "status")
    val status: String
) : Serializable
