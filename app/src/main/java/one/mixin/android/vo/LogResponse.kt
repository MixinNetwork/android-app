package one.mixin.android.vo

import com.google.gson.annotations.SerializedName

class LogResponse(
    val type: String,
    @SerializedName("log_id")
    val logId: String,
    val code: String,
    @SerializedName("ip_address")
    val ipAddress: String,
    @SerializedName("created_at")
    val createdAt: String
)
