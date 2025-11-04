package one.mixin.android.db.converter

import androidx.room.TypeConverter
import one.mixin.android.api.response.LimitOrderFundStatus
import one.mixin.android.api.response.LimitOrderStatus

class LimitOrderConverters {
    @TypeConverter
    fun fromStatus(value: String?): LimitOrderStatus? = value?.let { LimitOrderStatus.fromString(it) }

    @TypeConverter
    fun toStatus(value: LimitOrderStatus?): String? = value?.value

    @TypeConverter
    fun fromFundStatus(value: String?): LimitOrderFundStatus? = value?.let { LimitOrderFundStatus.fromString(it) }

    @TypeConverter
    fun toFundStatus(value: LimitOrderFundStatus?): String? = value?.value
}
