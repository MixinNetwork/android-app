package one.mixin.android.vo.sumsub

import com.google.gson.annotations.SerializedName

class RouteTokenResponse(
    @SerializedName("token")
    val token: String? = null,
    @SerializedName("state")
    val state: String,
)

enum class KycState(val value: String) {
    INITIAL("initial"),
    PENDING("pending"),
    SUCCESS("success"),
    RETRY("retry"),
    BLOCKED("blocked"),
    IGNORE("ignore"),
}
