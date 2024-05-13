package one.mixin.android.db.converter

import androidx.room.TypeConverter
import com.google.gson.reflect.TypeToken
import one.mixin.android.api.response.Web3Transfer
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.OldDepositEntry

class Web3TransferListConverter {
    @TypeConverter
    fun revertData(value: String?): List<Web3Transfer>? {
        val listType = object : TypeToken<ArrayList<Web3Transfer>>() {}.type
        return if (value.isNullOrBlank()) {
            emptyList()
        } else {
            GsonHelper.customGson.fromJson(value, listType)
        }
    }

    @TypeConverter
    fun converterData(vale: List<Web3Transfer>?): String? {
        if (vale == null) return null
        return GsonHelper.customGson.toJson(vale)
    }
}
