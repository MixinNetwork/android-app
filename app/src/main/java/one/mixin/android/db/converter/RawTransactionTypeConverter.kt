package one.mixin.android.db.converter

import androidx.room.TypeConverter
import one.mixin.android.vo.safe.RawTransactionType

class RawTransactionTypeConverter {
    @TypeConverter
    fun toRawTransactionType(value: Int): RawTransactionType {
        return RawTransactionType.entries.firstOrNull { it.value == value }
            ?: throw IllegalArgumentException("Invalid RawTransactionType value: $value")
    }

    @TypeConverter
    fun fromRawTransactionType(type: RawTransactionType): Int {
        return type.value
    }
}
