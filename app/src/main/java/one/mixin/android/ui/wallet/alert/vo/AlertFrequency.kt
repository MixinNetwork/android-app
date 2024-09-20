package one.mixin.android.ui.wallet.alert.vo

import com.google.gson.annotations.SerializedName
import one.mixin.android.R

enum class AlertFrequency(val value: String) {
    @SerializedName("every")
    EVERY("every"),

    @SerializedName("daily")
    DAILY("daily"),

    @SerializedName("once")
    ONCE("once");

    fun getStringResId(): Int {
        return when (this) {
            EVERY -> R.string.alert_frequency_every
            DAILY -> R.string.alert_frequency_daily
            ONCE -> R.string.alert_frequency_once
        }
    }
}
