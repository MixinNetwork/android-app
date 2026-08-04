package one.mixin.android.db.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import one.mixin.android.extension.equalsIgnoreCase

object DescriptionsConverter {
    @TypeConverter
    fun fromString(value: String?): Map<String, String>? {
        if (value.isNullOrEmpty() || value.equalsIgnoreCase("null")) return null
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return Gson().fromJson(value, mapType)
    }

    @TypeConverter
    fun fromMap(map: Map<String, String>?): String? {
        if (map.isNullOrEmpty()) return null
        return Gson().toJson(map)
    }
}
