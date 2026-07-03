package one.mixin.android.ui.wallet.alert.vo

import androidx.room3.ColumnTypeConverter

class AlertTypeConverter {
    @ColumnTypeConverter
    fun toType(value: String) = requireNotNull(AlertType.entries.firstOrNull { it.value == value })

    @ColumnTypeConverter
    fun fromType(value: AlertType) = value.value
}
