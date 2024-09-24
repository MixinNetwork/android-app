package one.mixin.android.ui.wallet.alert.vo

import androidx.room.TypeConverter

class AlertTypeConverter {
    @TypeConverter
    fun toType(value: String) = requireNotNull(AlertType.entries.firstOrNull { it.value == value })

    @TypeConverter
    fun fromType(value: AlertType) = value.value
}
