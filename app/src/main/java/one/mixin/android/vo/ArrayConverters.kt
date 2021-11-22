package one.mixin.android.vo

import androidx.room.TypeConverter
import com.squareup.moshi.Types
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter
import java.util.ArrayList

object ArrayConverters {
    @JvmStatic
    @TypeConverter
    fun fromString(value: String?): ArrayList<String> {
        if (value == null) return arrayListOf()
        return requireNotNull(jsonAdapter.fromJson(value))
    }

    @JvmStatic
    @TypeConverter
    fun fromArrayList(list: ArrayList<String>?): String {
        return jsonAdapter.toJson(list)
    }

    private val jsonAdapter by lazy {
        val listType = Types.newParameterizedType(ArrayList::class.java, String::class.java)
        getTypeAdapter<ArrayList<String>>(listType)
    }
}
