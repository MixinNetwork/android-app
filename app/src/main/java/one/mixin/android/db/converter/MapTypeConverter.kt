package one.mixin.android.db.converter

import androidx.room.TypeConverter
import com.google.common.reflect.TypeToken
import one.mixin.android.util.GsonHelper

class MapTypeConverter {
    private val gson = GsonHelper.customGson

    @TypeConverter
    fun fromMap(value: Map<String, String>?): String? {
        if (value == null) return null
        return gson.toJson(value)
    }

    @TypeConverter
    fun toMap(value: String?): Map<String, String>? {
        if (value == null) return null
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, type)
    }
}
