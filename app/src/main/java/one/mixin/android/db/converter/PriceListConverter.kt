package one.mixin.android.db.converter

import androidx.room3.ColumnTypeConverter
import com.google.gson.reflect.TypeToken
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.market.Price

class PriceListConverter {
    @ColumnTypeConverter
    fun revertDate(value: String?): List<Price> {
        val listType = object : TypeToken<ArrayList<Price>>() {}.type
        return if (value.isNullOrBlank()) {
            emptyList()
        } else {
            GsonHelper.customGson.fromJson(value, listType)
        }
    }

    @ColumnTypeConverter
    fun converterDate(list: List<Price>?): String? =
        if (list.isNullOrEmpty()) {
            null
        } else {
            GsonHelper.customGson.toJson(list)
        }
}
