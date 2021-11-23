package one.mixin.android.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class BlazeSignalKeyMessage(
    @Json(name = "message_id")
    val message_id: String,
    @Json(name = "recipient_id")
    val recipient_id: String,
    @Json(name = "data")
    val data: String,
    @Json(name = "session_id")
    val sessionId: String? = null
)

fun createBlazeSignalKeyMessage(recipientId: String, data: String, sessionId: String? = null) =
    BlazeSignalKeyMessage(UUID.randomUUID().toString(), recipientId, data, sessionId)
