package one.mixin.android.db.converter

import androidx.room.TypeConverter
import one.mixin.android.util.GsonHelper

class Web3FeeConverter {
    @TypeConverter
    fun revertData(value: String?): Web3FeeConverter? {
        if (value == null) return null
        return GsonHelper.customGson.fromJson(value, Web3FeeConverter::class.java)
    }


    @TypeConverter
    fun converterData(vale: Web3FeeConverter?): String? {
        if (vale == null) return null
        return GsonHelper.customGson.toJson(vale)
    }
}
