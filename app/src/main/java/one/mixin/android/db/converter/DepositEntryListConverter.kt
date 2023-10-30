package one.mixin.android.db.converter

import androidx.room.TypeConverter
import com.google.gson.reflect.TypeToken
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.OldDepositEntry

class DepositEntryListConverter {
    @TypeConverter
    fun revertDate(value: String?): List<OldDepositEntry> {
        val listType = object : TypeToken<ArrayList<OldDepositEntry>>() {}.type
        return if (value.isNullOrBlank()) {
            emptyList()
        } else {
            GsonHelper.customGson.fromJson(value, listType)
        }
    }

    @TypeConverter
    fun converterDate(list: List<OldDepositEntry>?): String = if (list.isNullOrEmpty()) {
        ""
    } else {
        GsonHelper.customGson.toJson(list)
    }
}
