package one.mixin.android.util

import androidx.room3.ColumnTypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object Converters {
    @ColumnTypeConverter
    fun fromString(value: String?): ArrayList<String> {
        val listType =
            object : TypeToken<ArrayList<String?>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @ColumnTypeConverter
    fun fromArrayList(list: ArrayList<String?>?): String {
        val gson = Gson()
        return gson.toJson(list)
    }
}
