package one.mixin.android.ui.wallet.alert.vo

import androidx.room3.ColumnTypeConverter

class AlertFrequencyConverter {
    @ColumnTypeConverter
    fun toFrequency(value: String) = requireNotNull(AlertFrequency.entries.firstOrNull { it.value == value })

    @ColumnTypeConverter
    fun fromFrequency(value: AlertFrequency) = value.value
}
