package one.mixin.android.db.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import one.mixin.android.extension.equalsIgnoreCase

object OptionalListConverter {
    @TypeConverter
    fun fromString(value: String?): List<String>? {
        if (value.isNullOrEmpty() || value.equalsIgnoreCase("null")) return null
        val listType = object : TypeToken<List<String?>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String?>?): String? {
        if (list.isNullOrEmpty()) return null
        val gson = Gson()
        return gson.toJson(list)
    }
}
