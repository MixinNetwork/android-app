package one.mixin.android.util

import androidx.collection.LruCache

class TimeCache private constructor(maxSize: Int) : LruCache<String, Any>(maxSize) {
    companion object {
        private const val TYPE_DATE = "date"
        private const val TYPE_TIME_AGO = "time_ago"
        private const val TYPE_TIME_AGO_DATE = "time_ago_date"
        private const val TYPE_HASH = "hash"
        private const val TYPE_CLOCK = "clock"

        val singleton: TimeCache by lazy { TimeCache((Runtime.getRuntime().maxMemory() / 32).toInt()) }
    }

    fun getDate(key: String): Any? {
        return get(key + TYPE_DATE)
    }

    fun putDate(
        key: String,
        value: String,
    ): Any? {
        return put(key + TYPE_DATE, value)
    }

    fun getTimeAgo(key: String): Any? {
        return get(key + TYPE_TIME_AGO)
    }

    fun putTimeAgo(
        key: String,
        value: String,
    ): Any? {
        return put(key + TYPE_TIME_AGO, value)
    }

    fun getTimeAgoDate(key: String): Any? {
        return get(key + TYPE_TIME_AGO_DATE)
    }

    fun putTimeAgoDate(
        key: String,
        value: String,
    ): Any? {
        return put(key + TYPE_TIME_AGO_DATE, value)
    }

    fun getHashForDate(key: String): Any? {
        return get(key + TYPE_HASH)
    }

    fun putHashForDate(
        key: String,
        value: Long,
    ): Any? {
        return put(key + TYPE_HASH, value)
    }

    fun getTimeAgoClock(key: String): Any? {
        return get(key + TYPE_CLOCK)
    }

    fun putTimeAgoClock(
        key: String,
        value: String,
    ): Any? {
        return put(key + TYPE_CLOCK, value)
    }
}
