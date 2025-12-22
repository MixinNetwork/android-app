package one.mixin.android.db.converter

import androidx.room.TypeConverter
import one.mixin.android.db.web3.vo.SafeChain

class SafeChainConverter {
    @TypeConverter
    fun toSafeChain(value: String?): SafeChain? {
        return SafeChain.fromValue(value)
    }

    @TypeConverter
    fun fromSafeChain(chain: SafeChain?): String? {
        return chain?.value
    }
}
