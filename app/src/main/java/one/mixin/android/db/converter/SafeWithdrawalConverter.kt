package one.mixin.android.db.converter

import androidx.room.TypeConverter
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.safe.SafeWithdrawal

class SafeWithdrawalConverter {
    @TypeConverter
    fun revertData(value: String?): SafeWithdrawal? {
        if (value == null) return null
        return GsonHelper.customGson.fromJson(value, SafeWithdrawal::class.java)
    }

    @TypeConverter
    fun converterData(status: SafeWithdrawal?): String? {
        return GsonHelper.customGson.toJson(status)
    }
}
