package one.mixin.android.ui.wallet.alert.vo

import one.mixin.android.R

enum class AlertFrequency(val value: String) {
    EVERY("every"),
    DAILY("daily"),
    ONCE("once");

    fun getStringResId(): Int {
        return when (this) {
            EVERY -> R.string.alert_frequency_every
            DAILY -> R.string.alert_frequency_daily
            ONCE -> R.string.alert_frequency_once
        }
    }
}
