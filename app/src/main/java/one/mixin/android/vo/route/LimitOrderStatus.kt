package one.mixin.android.vo.route;

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
