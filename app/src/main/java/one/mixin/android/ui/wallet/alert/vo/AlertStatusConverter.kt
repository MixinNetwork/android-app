package one.mixin.android.ui.wallet.alert.vo

import androidx.room.TypeConverter

class AlertStatusConverter {
    @TypeConverter
    fun toFrequency(value: String) = requireNotNull(AlertStatus.entries.firstOrNull { it.value == value })

    @TypeConverter
    fun fromFrequency(value: AlertStatus) = value.value
}
