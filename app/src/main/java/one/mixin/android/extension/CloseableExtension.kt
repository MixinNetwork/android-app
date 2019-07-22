package one.mixin.android.extension

import java.io.Closeable
import java.io.IOException

fun Closeable.closeSilently() {
    try {
        close()
    } catch (e: IOException) {
        e.printStackTrace()
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
    }
}
