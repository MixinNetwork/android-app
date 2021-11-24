package one.mixin.android.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class BlazeMessageData(
    @Json(name = "conversation_id")
    val conversationId: String,
    @Json(name = "user_id")
    var userId: String,
    @Json(name = "message_id")
    var messageId: String,
    @Json(name = "category")
    val category: String,
    @Json(name = "data")
    val data: String,
    @Json(name = "status")
    val status: String,
    @Json(name = "created_at")
    val createdAt: String,
    @Json(name = "updated_at")
    val updatedAt: String,
    @Json(name = "source")
    val source: String,
    @Json(name = "representative_id")
    val representativeId: String?,
    @Json(name = "quote_message_id")
    val quoteMessageId: String?,
    @Json(name = "session_id")
    val sessionId: String,
    @Json(name = "silent")
    val silent: Boolean? = null,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 5L
    }
}
