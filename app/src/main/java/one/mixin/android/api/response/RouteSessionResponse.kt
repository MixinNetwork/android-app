package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

class RouteSessionResponse(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("session_secret")
    val sessionSecret: String,
    @SerializedName("instrument_id")
    val instrumentId: String,
    @SerializedName("scheme")
    val scheme: String,
    @SerializedName("last4")
    val last4: String,
    @SerializedName("amount")
    val amount: Long,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("status")
    var status: String,
)

enum class RouteSessionStatus(val value: String) {
    Pending("pending"),
    Processing("processing"),
    Challenged("challenged"),
    ChallengeAbandoned("challenge_abandoned"),
    Expired("expired"),
    Approved("approved"),
    Attempted("attempted"),
    Unavailable("unavailable"),
    Declined("declined"),
    Rejected("rejected"),
}
