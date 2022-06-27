package one.mixin.android.db.converter

import androidx.room.TypeConverter
import com.google.gson.reflect.TypeToken
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.DepositEntry

class DepositEntryListConverter {
    @TypeConverter
    fun revertDate(value: String?): List<DepositEntry> {
        val listType = object : TypeToken<ArrayList<DepositEntry>>() {}.type
        return if (value.isNullOrBlank()) {
            emptyList()
        } else {
            GsonHelper.customGson.fromJson(value, listType)
        }
    }

    @TypeConverter
    fun converterDate(list: List<DepositEntry>?): String = if (list.isNullOrEmpty()) {
        ""
    } else {
        GsonHelper.customGson.toJson(list)
    }
}
