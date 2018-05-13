package one.mixin.android.vo

import android.arch.lifecycle.LiveData

class LinkState : LiveData<Int>() {
    companion object {
        const val ONLINE = 0
        const val OFFLINE = 1

        fun isOnline(state: Int?) = ONLINE == state
    }

    var state: Int? = null
        set(value) {
            if (field != value) {
                field = value
                setValue(state)
            }
        }
}
