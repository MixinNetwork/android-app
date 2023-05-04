package one.mixin.android.extension

fun ByteArray?.isNullOrEmpty(): Boolean {
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
