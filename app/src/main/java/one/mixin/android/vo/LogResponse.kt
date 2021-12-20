package one.mixin.android.vo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class LogResponse(
    val type: String,
    @Json(name = "log_id")
    val logId: String,
    val code: String,
    @Json(name = "ip_address")
    val ipAddress: String,
    @Json(name = "created_at")
    val createdAt: String
)
