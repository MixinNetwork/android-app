package one.mixin.android.db.converter

import androidx.room.TypeConverter
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.safe.Treasury

class TreasuryConverter {
    @TypeConverter
    fun revertData(value: String?): Treasury? {
        if (value == null) return null
        return GsonHelper.customGson.fromJson(value, Treasury::class.java)
    }

    @TypeConverter
    fun converterData(status: Treasury?): String? {
        return GsonHelper.customGson.toJson(status)
    }
}
