package one.mixin.android.webrtc

import androidx.lifecycle.LiveData

class CallDebugLiveData : LiveData<CallDebugLiveData.Type>() {
    var debugState = Type.None
        set(value) {
            if (field != value) {
                field = value
                postValue(value)
            }
        }

    enum class Type {
        None, Log, Report, All;

        fun logEnable() = this == Log || this == All
        fun reportEnable() = this == Report || this == All
    }

    fun reset() {
        debugState = Type.None
    }
}

inline fun <reified T : Enum<T>> T.next(): T {
    val values = enumValues<T>()
    val nextOrdinal = (ordinal + 1) % values.size
    return values[nextOrdinal]
}
