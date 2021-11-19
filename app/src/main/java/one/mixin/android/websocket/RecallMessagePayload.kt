package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.util.MoshiHelper.getTypeAdapter

@JsonClass(generateAdapter = true)
data class RecallMessagePayload(
    @SerializedName("message_id")
    @Json(name = "message_id")
    val messageId: String
)

fun RecallMessagePayload.toJson(): String = getTypeAdapter<RecallMessagePayload>(RecallMessagePayload::class.java).toJson(this)
