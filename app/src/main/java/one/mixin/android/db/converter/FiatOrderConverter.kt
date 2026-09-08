package one.mixin.android.db.converter

import androidx.room3.ColumnTypeConverter
import com.google.gson.reflect.TypeToken
import one.mixin.android.api.response.FiatOrderView
import one.mixin.android.util.GsonHelper

class FiatOrderConverter {
    @ColumnTypeConverter
    fun fromFiatOrder(fiatOrder: FiatOrderView?): String? {
        if (fiatOrder == null) return null
        return GsonHelper.customGson.toJson(fiatOrder)
    }

    @ColumnTypeConverter
    fun toFiatOrder(json: String?): FiatOrderView? {
        if (json == null) return null
        return GsonHelper.customGson.fromJson(json, object : TypeToken<FiatOrderView>() {}.type)
    }
}
