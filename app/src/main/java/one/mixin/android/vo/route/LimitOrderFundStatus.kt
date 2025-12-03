package one.mixin.android.vo.route;

import com.google.gson.annotations.SerializedName;

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
