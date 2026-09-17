package one.mixin.android.ui.wallet.alert.vo

import androidx.room3.ColumnTypeConverter

class AlertStatusConverter {
    @ColumnTypeConverter
    fun toFrequency(value: String) = requireNotNull(AlertStatus.entries.firstOrNull { it.value == value })

    @ColumnTypeConverter
    fun fromFrequency(value: AlertStatus) = value.value
}
