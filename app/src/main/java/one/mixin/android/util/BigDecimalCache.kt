package one.mixin.android.util

import android.util.LruCache

class BigDecimalCache private constructor(maxSize: Int): LruCache<String, String>(maxSize) {

    companion object {
        const val FORMAT_TYPE_0 = 0
        const val FORMAT_TYPE_2 = 1
        const val FORMAT_TYPE_8 = 2

        val SINGLETON: BigDecimalCache by lazy {
            BigDecimalCache((Runtime.getRuntime().maxMemory() / 128).toInt())
        }
    }
}