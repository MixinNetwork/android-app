package one.mixin.android.db.converter

import androidx.room3.ColumnTypeConverter
import com.google.gson.reflect.TypeToken
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.OldDepositEntry

class DepositEntryListConverter {
    @ColumnTypeConverter
    fun revertDate(value: String?): List<OldDepositEntry> {
        val listType = object : TypeToken<ArrayList<OldDepositEntry>>() {}.type
        return if (value.isNullOrBlank()) {
            emptyList()
        } else {
            GsonHelper.customGson.fromJson(value, listType)
        }
    }

    @ColumnTypeConverter
    fun converterDate(list: List<OldDepositEntry>?): String =
        if (list.isNullOrEmpty()) {
            ""
        } else {
            GsonHelper.customGson.toJson(list)
        }
}
