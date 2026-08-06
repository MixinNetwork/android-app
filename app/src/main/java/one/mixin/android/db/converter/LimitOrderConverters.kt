package one.mixin.android.db.converter

import androidx.room3.ColumnTypeConverter
import one.mixin.android.vo.route.LimitOrderFundStatus
import one.mixin.android.vo.route.LimitOrderStatus

class LimitOrderConverters {
    @ColumnTypeConverter
    fun fromStatus(value: String?): LimitOrderStatus? = value?.let { LimitOrderStatus.fromString(it) }

    @ColumnTypeConverter
    fun toStatus(value: LimitOrderStatus?): String? = value?.value

    @ColumnTypeConverter
    fun fromFundStatus(value: String?): LimitOrderFundStatus? = value?.let { LimitOrderFundStatus.fromString(it) }

    @ColumnTypeConverter
    fun toFundStatus(value: LimitOrderFundStatus?): String? = value?.value
}
