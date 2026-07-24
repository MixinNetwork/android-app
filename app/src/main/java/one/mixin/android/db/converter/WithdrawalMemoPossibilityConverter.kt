package one.mixin.android.db.converter

import androidx.room3.ColumnTypeConverter
import one.mixin.android.vo.WithdrawalMemoPossibility

class WithdrawalMemoPossibilityConverter {
    @ColumnTypeConverter
    fun revertDate(value: String?): WithdrawalMemoPossibility? {
        if (value == null) return null
        return requireNotNull(WithdrawalMemoPossibility.values().firstOrNull { it.value == value })
    }

    @ColumnTypeConverter
    fun converterDate(status: WithdrawalMemoPossibility?): String? {
        return status?.value
    }
}
