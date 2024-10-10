package one.mixin.android.ui.wallet.alert.vo

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import one.mixin.android.R

enum class AlertFrequency(val value: String) {
    @SerializedName("once")
    ONCE("once"),

    @SerializedName("daily")
    DAILY("daily"),

    @SerializedName("every")
    EVERY("every");

    @StringRes
    fun getTitleResId(): Int {
        return when (this) {
            EVERY -> R.string.alert_frequency_every
            DAILY -> R.string.alert_frequency_daily
            ONCE -> R.string.alert_frequency_once
        }
    }

    @StringRes
    fun getSubTitleResId(): Int {
        return when (this) {
            EVERY -> R.string.alert_frequency_every_description
            DAILY -> R.string.alert_frequency_daily_description
            ONCE -> R.string.alert_frequency_once_description
        }
    }

    @DrawableRes
    fun getIconResId(): Int {
        return when (this) {
            EVERY -> R.drawable.ic_frequency_every
            DAILY -> R.drawable.ic_frequency_daily
            ONCE -> R.drawable.ic_frequency_once
        }
    }
}
