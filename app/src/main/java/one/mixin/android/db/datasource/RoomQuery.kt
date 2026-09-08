package one.mixin.android.db.datasource

import androidx.room3.RoomRawQuery
import androidx.sqlite.SQLiteStatement

class RoomQuery private constructor(
    val sql: String,
    private val bindArgs: Array<Any?>,
) {
    val argCount: Int
        get() = bindArgs.size

    fun bindNull(index: Int) {
        bindArgs[index - 1] = null
    }

    fun bindLong(index: Int, value: Long) {
        bindArgs[index - 1] = value
    }

    fun bindDouble(index: Int, value: Double) {
        bindArgs[index - 1] = value
    }

    fun bindString(index: Int, value: String) {
        bindArgs[index - 1] = value
    }

    fun bindBlob(index: Int, value: ByteArray) {
        bindArgs[index - 1] = value
    }

    fun copyArgumentsFrom(other: RoomQuery) {
        val count = minOf(bindArgs.size, other.bindArgs.size)
        for (i in 0 until count) {
            bindArgs[i] = other.bindArgs[i]
        }
    }

    fun release() = Unit

    fun toRoomRawQuery(): RoomRawQuery {
        return RoomRawQuery(sql) { statement ->
            bindArgs.forEachIndexed { index, value ->
                statement.bindArg(index + 1, value)
            }
        }
    }

    fun bindTo(statement: SQLiteStatement) {
        bindArgs.forEachIndexed { index, value ->
            statement.bindArg(index + 1, value)
        }
    }

    fun withLimitOffset(
        limit: Int,
        offset: Int,
    ): RoomQuery {
        val args = bindArgs.copyOf(bindArgs.size + 2)
        args[bindArgs.size] = limit.toLong()
        args[bindArgs.size + 1] = offset.toLong()
        return RoomQuery("$sql LIMIT ? OFFSET ?", args)
    }

    companion object {
        @JvmStatic
        fun acquire(
            sql: String,
            argCount: Int,
        ): RoomQuery = RoomQuery(sql, arrayOfNulls(argCount))

        @JvmStatic
        fun copyFrom(other: RoomQuery): RoomQuery {
            return RoomQuery(other.sql, other.bindArgs.copyOf())
        }
    }
}

fun SQLiteStatement.bindArg(
    index: Int,
    value: Any?,
) {
    when (value) {
        null -> bindNull(index)
        is ByteArray -> bindBlob(index, value)
        is Float -> bindDouble(index, value.toDouble())
        is Double -> bindDouble(index, value)
        is Number -> bindLong(index, value.toLong())
        is Boolean -> bindLong(index, if (value) 1L else 0L)
        else -> bindText(index, value.toString())
    }
}
