package one.mixin.android.db.converter

import androidx.room.TypeConverter
import one.mixin.android.vo.safe.RawTransactionState
import java.util.Locale

class RawTransactionStateConverter {
    @TypeConverter
    fun toRawTransactionState(value: String): RawTransactionState {
        return RawTransactionState.valueOf(value.lowercase(Locale.US))
    }

    @TypeConverter
    fun fromRawTransactionState(state: RawTransactionState): String {
        return state.name.lowercase(Locale.US)
    }
}
