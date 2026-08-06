package one.mixin.android.db.datasource

import android.database.AbstractCursor
import android.database.Cursor
import androidx.sqlite.SQLiteStatement

internal class SQLiteStatementCursor private constructor(
    private val names: Array<String>,
    private val rows: List<Array<Any?>>,
) : AbstractCursor() {
    override fun getCount(): Int = rows.size

    override fun getColumnNames(): Array<String> = names

    override fun getString(column: Int): String? {
        return value(column)?.let {
            when (it) {
                is ByteArray -> String(it)
                else -> it.toString()
            }
        }
    }

    override fun getShort(column: Int): Short = getLong(column).toShort()

    override fun getInt(column: Int): Int = getLong(column).toInt()

    override fun getLong(column: Int): Long {
        return when (val value = value(column)) {
            null -> 0L
            is Number -> value.toLong()
            is Boolean -> if (value) 1L else 0L
            else -> value.toString().toLong()
        }
    }

    override fun getFloat(column: Int): Float = getDouble(column).toFloat()

    override fun getDouble(column: Int): Double {
        return when (val value = value(column)) {
            null -> 0.0
            is Number -> value.toDouble()
            is Boolean -> if (value) 1.0 else 0.0
            else -> value.toString().toDouble()
        }
    }

    override fun getBlob(column: Int): ByteArray? {
        return when (val value = value(column)) {
            null -> null
            is ByteArray -> value
            else -> value.toString().toByteArray()
        }
    }

    override fun isNull(column: Int): Boolean = value(column) == null

    override fun getType(column: Int): Int {
        return when (value(column)) {
            null -> Cursor.FIELD_TYPE_NULL
            is ByteArray -> Cursor.FIELD_TYPE_BLOB
            is Float, is Double -> Cursor.FIELD_TYPE_FLOAT
            is Number, is Boolean -> Cursor.FIELD_TYPE_INTEGER
            else -> Cursor.FIELD_TYPE_STRING
        }
    }

    private fun value(column: Int): Any? = rows[position][column]

    companion object {
        fun from(statement: SQLiteStatement): Cursor {
            statement.use {
                val columnCount = it.getColumnCount()
                val columnNames = Array(columnCount) { index -> it.getColumnName(index) }
                val rows = mutableListOf<Array<Any?>>()
                while (it.step()) {
                    rows += Array(columnCount) { index -> it.readValue(index) }
                }
                return SQLiteStatementCursor(columnNames, rows)
            }
        }

        private fun SQLiteStatement.readValue(index: Int): Any? {
            if (isNull(index)) return null
            return when (getColumnType(index)) {
                Cursor.FIELD_TYPE_BLOB -> getBlob(index)
                Cursor.FIELD_TYPE_FLOAT -> getDouble(index)
                Cursor.FIELD_TYPE_INTEGER -> getLong(index)
                else -> getText(index)
            }
        }
    }
}

fun SQLiteStatement.toCursor(): Cursor = SQLiteStatementCursor.from(this)
