package one.mixin.android.db.converter

import androidx.room.TypeConverter
import one.mixin.android.vo.WithdrawalMemoPossibility

class WithdrawalMemoPossibilityConverter {
    @TypeConverter
    fun revertDate(value: String?): WithdrawalMemoPossibility? {
        if (value == null) return null
        return requireNotNull(WithdrawalMemoPossibility.values().firstOrNull { it.value == value })
    }

    @TypeConverter
    fun converterDate(status: WithdrawalMemoPossibility?): String? {
        return status?.value
    }
}
