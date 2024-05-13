package one.mixin.android.db.converter

import androidx.room.TypeConverter
import com.google.gson.reflect.TypeToken
import one.mixin.android.api.response.Approval
import one.mixin.android.util.GsonHelper

class ApprovalConverter {
    @TypeConverter
    fun revertData(value: String?): List<Approval>? {
        val listType = object : TypeToken<ArrayList<Approval>>() {}.type
        return if (value.isNullOrBlank()) {
            emptyList()
        } else {
            GsonHelper.customGson.fromJson(value, listType)
        }
    }

    @TypeConverter
    fun converterData(vale: List<Approval>?): String? {
        if (vale == null) return null
        return GsonHelper.customGson.toJson(vale)
    }
}
