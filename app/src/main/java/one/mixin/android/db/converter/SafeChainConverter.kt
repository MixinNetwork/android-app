package one.mixin.android.db.converter

import androidx.room3.ColumnTypeConverter
import one.mixin.android.db.web3.vo.SafeChain

class SafeChainConverter {
    @ColumnTypeConverter
    fun toSafeChain(value: String?): SafeChain? {
        return SafeChain.fromValue(value)
    }

    @ColumnTypeConverter
    fun fromSafeChain(chain: SafeChain?): String? {
        return chain?.value
    }
}
