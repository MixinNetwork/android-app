package one.mixin.android.db.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import one.mixin.android.db.web3.vo.AssetChange

class AssetChangeListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromAssetChangeList(value: List<AssetChange>?): String {
        return if (value == null) {
            "[]"
        } else {
            gson.toJson(value)
        }
    }

    @TypeConverter
    fun toAssetChangeList(value: String): List<AssetChange>? {
        val listType = object : TypeToken<List<AssetChange>>() {}.type
        return try {
            gson.fromJson(value, listType)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
