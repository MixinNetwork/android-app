package one.mixin.android.db.converter

import androidx.room3.ColumnTypeConverter
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.Membership

class MembershipConverter {
    @ColumnTypeConverter
    fun revertData(value: String?): Membership? {
        if (value == null) return null
        return GsonHelper.customGson.fromJson(value, Membership::class.java)
    }

    @ColumnTypeConverter
    fun converterData(treasury: Membership?): String? {
        if (treasury == null) return null
        return GsonHelper.customGson.toJson(treasury)
    }
}
