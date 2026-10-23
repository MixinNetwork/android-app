package one.mixin.android.db.datasource

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.SQLiteConnection

fun SQLiteConnection.execSQL(sql: String) {
    prepare(sql).use { it.step() }
}

fun SQLiteConnection.execSQL(
    sql: String,
    bindArgs: Array<Any?>,
) {
    prepare(sql).use { statement ->
        bindArgs.forEachIndexed { index, value ->
            statement.bindArg(index + 1, value)
        }
        statement.step()
    }
}

fun SQLiteConnection.query(sql: String): Cursor = prepare(sql).toCursor()

fun SQLiteConnection.insert(
    table: String,
    conflictAlgorithm: Int,
    values: ContentValues,
): Long {
    val columns = values.keySet().toList()
    val conflictClause =
        when (conflictAlgorithm) {
            SQLiteDatabase.CONFLICT_ROLLBACK -> "OR ROLLBACK "
            SQLiteDatabase.CONFLICT_ABORT -> "OR ABORT "
            SQLiteDatabase.CONFLICT_FAIL -> "OR FAIL "
            SQLiteDatabase.CONFLICT_IGNORE -> "OR IGNORE "
            SQLiteDatabase.CONFLICT_REPLACE -> "OR REPLACE "
            else -> ""
        }
    val sql =
        if (columns.isEmpty()) {
            "INSERT ${conflictClause}INTO `$table` DEFAULT VALUES"
        } else {
            val columnList = columns.joinToString(", ") { "`$it`" }
            val placeholders = columns.joinToString(", ") { "?" }
            "INSERT ${conflictClause}INTO `$table` ($columnList) VALUES ($placeholders)"
        }
    prepare(sql).use { statement ->
        columns.forEachIndexed { index, column ->
            statement.bindArg(index + 1, values.get(column))
        }
        statement.step()
    }
    return lastInsertRowId()
}

private fun SQLiteConnection.lastInsertRowId(): Long {
    return prepare("SELECT last_insert_rowid()").use { statement ->
        if (statement.step()) statement.getLong(0) else -1L
    }
}
