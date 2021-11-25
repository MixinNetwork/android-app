package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class BlazeMessageData(
    @SerializedName("conversation_id")
    @Json(name = "conversation_id")
    val conversationId: String,
    @SerializedName("user_id")
    @Json(name = "user_id")
    var userId: String,
    @SerializedName("message_id")
    @Json(name = "message_id")
    var messageId: String,
    @SerializedName("category")
    @Json(name = "category")
    val category: String,
    @SerializedName("data")
    @Json(name = "data")
    val data: String,
    @SerializedName("status")
    @Json(name = "status")
    val status: String,
    @SerializedName("created_at")
    @Json(name = "created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    @Json(name = "updated_at")
    val updatedAt: String,
    @SerializedName("source")
    @Json(name = "source")
    val source: String,
    @SerializedName("representative_id")
    @Json(name = "representative_id")
    val representativeId: String?,
    @SerializedName("quote_message_id")
    @Json(name = "quote_message_id")
    val quoteMessageId: String?,
    @SerializedName("session_id")
    @Json(name = "session_id")
    val sessionId: String,
    @SerializedName("silent")
    @Json(name = "silent")
    val silent: Boolean? = null,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 5L
    }
}
