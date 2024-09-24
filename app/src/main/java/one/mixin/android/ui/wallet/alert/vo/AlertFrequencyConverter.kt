package one.mixin.android.ui.wallet.alert.vo

import androidx.room.TypeConverter

class AlertFrequencyConverter {
    @TypeConverter
    fun toFrequency(value: String) = requireNotNull(AlertFrequency.entries.firstOrNull { it.value == value })

    @TypeConverter
    fun fromFrequency(value: AlertFrequency) = value.value
}
