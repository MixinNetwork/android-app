package one.mixin.android.vo.checkout

import com.google.gson.annotations.SerializedName



data class TraceResponse(
    @SerializedName("trace_id")
    val traceID: String
)