package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

class RouteOrderResponse(
    @SerializedName("order_id")
    val orderId: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("asset_amount")
    val assetAmount: String,
    @SerializedName("price")
    val price: String,
    @SerializedName("instrument")
    val instrument: Instrument,
    @SerializedName("instrument_id")
    val instrumentId: String,
    @SerializedName("session")
    val session: Session,
    @SerializedName("state")
    val state: String,
    @SerializedName("reason")
    val reason: String
)

class Instrument(
    @SerializedName("scheme")
    val scheme: String,
    @SerializedName("last4")
    val last4: String,
)

class Session(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("session_secret")
    val sessionSecret: String,
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
