package one.mixin.android.db.converter

import androidx.room3.ColumnTypeConverter
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.safe.Treasury

class TreasuryConverter {
    @ColumnTypeConverter
    fun revertData(value: String?): Treasury? {
        if (value == null) return null
        return GsonHelper.customGson.fromJson(value, Treasury::class.java)
    }

    @ColumnTypeConverter
    fun converterData(treasury: Treasury?): String? {
        if (treasury == null) return null
        return GsonHelper.customGson.toJson(treasury)
    }
}
