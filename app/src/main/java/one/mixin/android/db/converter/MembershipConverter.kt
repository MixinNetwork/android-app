package one.mixin.android.db.converter

import androidx.room.TypeConverter
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.Membership

class MembershipConverter {
    @TypeConverter
    fun revertData(value: String?): Membership? {
        if (value == null) return null
        return GsonHelper.customGson.fromJson(value, Membership::class.java)
    }

    @TypeConverter
    fun converterData(treasury: Membership?): String? {
        if (treasury == null) return null
        return GsonHelper.customGson.toJson(treasury)
    }
}
