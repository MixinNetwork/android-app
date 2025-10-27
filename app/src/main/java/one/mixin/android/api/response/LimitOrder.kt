package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

enum class LimitOrderStatus(val value: String) {
    @SerializedName("created")
    CREATED("created"),

    @SerializedName("pricing")
    PRICING("pricing"),

    @SerializedName("quoting")
    QUOTING("quoting"),

    @SerializedName("expired")
    EXPIRED("expired"),

    @SerializedName("settled")
    SETTLED("settled"),

    @SerializedName("cancelled")
    CANCELLED("cancelled"),

    @SerializedName("failed")
    FAILED("failed"),

    UNKNOWN("unknown");

    companion object {
        fun fromString(value: String): LimitOrderStatus {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

enum class LimitOrderCategory(val value: String) {
    @SerializedName("all")
    ALL("all"),

    @SerializedName("active")
    ACTIVE("active"),

    @SerializedName("history")
    HISTORY("history");

    companion object {
        fun fromString(value: String): LimitOrderCategory {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: ALL
        }
    }
}

enum class LimitOrderFundStatus(val value: String) {
    @SerializedName("pending")
    PENDING("pending"), // waiting user to transfer

    @SerializedName("held")
    HELD("held"), // user has paid, holding the fund

    @SerializedName("transferring")
    TRANSFERRING("transferring"), // transferring the fund to user

    @SerializedName("transferred")
    TRANSFERRED("transferred"), // fund has been transferred to user

    UNKNOWN("unknown");

    companion object {
        fun fromString(value: String): LimitOrderFundStatus {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

data class LimitOrder(
    @SerializedName("limit_order_id")
    val limitOrderId: String,
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("receive_asset_id")
    val receiveAssetId: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("pending_amount")
    val pendingAmount: String,
    @SerializedName("expected_receive_amount")
    val expectedReceiveAmount: String,
    @SerializedName("price")
    val price: String,
    @SerializedName("state")
    val state: LimitOrderStatus,
    @SerializedName("fund_status")
    val fundStatus: LimitOrderFundStatus,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("expired_at")
    val expiredAt: String
)
