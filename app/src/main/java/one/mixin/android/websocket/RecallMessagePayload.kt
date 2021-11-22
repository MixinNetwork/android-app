package one.mixin.android.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter

@JsonClass(generateAdapter = true)
data class RecallMessagePayload(
    @Json(name ="message_id")
    val messageId: String
)

fun RecallMessagePayload.toJson(): String = getTypeAdapter<RecallMessagePayload>(RecallMessagePayload::class.java).toJson(this)
