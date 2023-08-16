package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class CheckoutSessionRequest(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("session_secret")
    val sessionSecret: String,
    @SerializedName("instrument_id")
    val instrumentId: String?,
    @SerializedName("amount")
    val amount: Int,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("scheme")
    var scheme: String,
    @SerializedName("status")
    var status: String,
)

enum class SessionStatus(val value: String) {
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
