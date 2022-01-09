package one.mixin.android.extension

fun isNullOrEmpty(byteArray: ByteArray?): Boolean {
    return byteArray == null || byteArray.isEmpty()
}

inline fun <T> Array<T>.forEachReversedWithIndex(f: (Int, T) -> Unit) {
    var i = size - 1
    while (i >= 0) {
        f(i, get(i))
        i--
    }
}
