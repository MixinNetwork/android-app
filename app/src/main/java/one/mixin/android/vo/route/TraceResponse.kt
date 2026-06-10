package one.mixin.android.vo.route

import com.google.gson.annotations.SerializedName

data class TraceResponse(
    @SerializedName("trace_id")
    val traceID: String,
)
