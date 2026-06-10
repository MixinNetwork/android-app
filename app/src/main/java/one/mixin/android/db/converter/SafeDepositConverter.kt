package one.mixin.android.db.converter

import androidx.room.TypeConverter
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.safe.SafeDeposit

class SafeDepositConverter {
    @TypeConverter
    fun revertData(value: String?): SafeDeposit? {
        if (value == null) return null
        return GsonHelper.customGson.fromJson(value, SafeDeposit::class.java)
    }

    @TypeConverter
    fun converterData(status: SafeDeposit?): String? {
        return GsonHelper.customGson.toJson(status)
    }
}
