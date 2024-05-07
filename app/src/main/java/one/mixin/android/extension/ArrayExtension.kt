package one.mixin.android.extension

import kotlin.contracts.contract

fun ByteArray?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }
    return this == null || this.isEmpty()
}

inline fun <T> Array<T>.forEachReversedWithIndex(f: (Int, T) -> Unit) {
    var i = size - 1
    while (i >= 0) {
        f(i, get(i))
        i--
    }
}

inline fun <T> List<T>.forEachReversedWithIndex(f: (Int, T) -> Unit) {
    var i = size - 1
    while (i >= 0) {
        f(i, get(i))
        i--
    }
}

inline fun <T> List<T>.forEachWithIndex(f: (Int, T) -> Unit) {
    var i = 0
    while (i < size) {
        f(i, get(i))
        i++
    }
}
