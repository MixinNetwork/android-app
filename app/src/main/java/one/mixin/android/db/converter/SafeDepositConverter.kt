package one.mixin.android.db.converter

import androidx.room3.ColumnTypeConverter
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.safe.SafeDeposit

class SafeDepositConverter {
    @ColumnTypeConverter
    fun revertData(value: String?): SafeDeposit? {
        if (value == null) return null
        return GsonHelper.customGson.fromJson(value, SafeDeposit::class.java)
    }

    @ColumnTypeConverter
    fun converterData(status: SafeDeposit?): String? {
        return GsonHelper.customGson.toJson(status)
    }
}
