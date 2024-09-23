package one.mixin.android.ui.wallet.alert.vo

import com.google.gson.annotations.SerializedName

enum class AlertStatus(val value: String) {
    @SerializedName("PAUSED")
    PAUSED("PAUSED"),

    @SerializedName("RUNNING")
    RUNNING("RUNNING");
}
