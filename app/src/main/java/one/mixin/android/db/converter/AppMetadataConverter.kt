package one.mixin.android.db.converter

import androidx.room.TypeConverter
import one.mixin.android.api.response.AppMetadata
import one.mixin.android.util.GsonHelper

class AppMetadataConverter {
    @TypeConverter
    fun revertData(value: String?): AppMetadata? {
        if (value == null) return null
        return GsonHelper.customGson.fromJson(value, AppMetadata::class.java)
    }

    @TypeConverter
    fun converterData(status: AppMetadata?): String? {
        return GsonHelper.customGson.toJson(status)
    }
}
