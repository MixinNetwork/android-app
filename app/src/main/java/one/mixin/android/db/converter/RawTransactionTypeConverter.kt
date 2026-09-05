package one.mixin.android.db.converter

import androidx.room3.ColumnTypeConverter
import one.mixin.android.vo.safe.RawTransactionType

class RawTransactionTypeConverter {
    @ColumnTypeConverter
    fun toRawTransactionType(value: Int): RawTransactionType {
        return RawTransactionType.entries.firstOrNull { it.value == value }
            ?: throw IllegalArgumentException("Invalid RawTransactionType value: $value")
    }

    @ColumnTypeConverter
    fun fromRawTransactionType(type: RawTransactionType): Int {
        return type.value
    }
}
