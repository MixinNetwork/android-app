package one.mixin.android.ui.wallet.alert.vo

import com.google.gson.annotations.SerializedName

enum class AlertStatus(val value: String) {
    @SerializedName("paused")
    PAUSED("paused"),

    @SerializedName("running")
    RUNNING("running");
}
