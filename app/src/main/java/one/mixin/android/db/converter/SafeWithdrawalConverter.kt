package one.mixin.android.db.converter

import androidx.room3.ColumnTypeConverter
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.safe.SafeWithdrawal

class SafeWithdrawalConverter {
    @ColumnTypeConverter
    fun revertData(value: String?): SafeWithdrawal? {
        if (value == null) return null
        return GsonHelper.customGson.fromJson(value, SafeWithdrawal::class.java)
    }

    @ColumnTypeConverter
    fun converterData(status: SafeWithdrawal?): String? {
        return GsonHelper.customGson.toJson(status)
    }
}
