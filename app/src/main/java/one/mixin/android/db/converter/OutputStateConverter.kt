package one.mixin.android.db.converter

import androidx.room3.ColumnTypeConverter
import one.mixin.android.vo.safe.OutputState
import java.util.Locale

class OutputStateConverter {
    @ColumnTypeConverter
    fun toOutputState(value: String): OutputState {
        return OutputState.valueOf(value.lowercase(Locale.US))
    }

    @ColumnTypeConverter
    fun fromOutputState(state: OutputState): String {
        return state.name.lowercase(Locale.US)
    }
}
