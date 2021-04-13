package one.mixin.android.ui.repair

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.util.regex.Pattern
import kotlin.experimental.and

fun dumpSqliteMaster(db: SQLiteDatabase, dumpFile: File) {
    dumpFile.bufferedWriter().use { writer ->
        try {
            writer.write("# version: ${db.version}\n")

            val tables = arrayListOf<String>()
            val cursor = db.rawQuery(
                "SELECT name, type, sql FROM sqlite_master WHERE sql NOT NULL AND type='table'",
                null
            )
            cursor.use { c ->
                if (!c.moveToFirst()) return

                while (c.moveToNext()) {
                    val name = c.getString(c.getColumnIndexOrThrow("name"))
                    if (name == "android_metadata") continue

                    Timber.d("@@@ dump table: $name")
                    tables.add(name)
                }
            }

            for (t in tables) {
                try {
                    dumpTable(db, writer, t)
                } catch (e: Exception) {
                    Timber.e("@@@ Failed to dump table($t)")
                }
            }
        } catch (e: Exception) {
            Timber.e("@@@ Failed to query table info from sqlite_master")
        }

        try {
            val cursor = db.rawQuery(
                "SELECT sql FROM sqlite_master WHERE sql NOT NULL AND type IN ('index','trigger','view')",
                null
            )
            cursor.use { c ->
                if (!c.moveToFirst()) return

                while (c.moveToNext()) {
                    val sql = c.getString(0)
                    Timber.d("@@@ dump : $sql")
                    writer.write("$sql\n")
                }
            }
        } catch (e: Exception) {
            Timber.e("@@@ Failed to query index, trigger, view from sqlite_master")
        }
    }
}

fun recoverSqliteMaster(db: SQLiteDatabase, dumpFile: File) {
    dumpFile.bufferedReader().use { reader ->
        var line = reader.readLine() ?: return

        val pattern = Pattern.compile("#\\s*version:\\s*(\\d+)")
        val matcher = pattern.matcher(line)
        if (!matcher.matches()) {
            Timber.e("@@@ Failed to read version from dumpFile")
        }
        val version = matcher.group(1)?.toIntOrNull()
        if (version == null) {
            Timber.e("@@@ Failed to read version from dumpFile")
        } else {
            db.version = version
        }

        while (reader.readLine()?.also { line = it } != null) {
            if (line.startsWith("#")) {
                continue
            }
            Timber.d("@@@ executing: $line")
            try {
                db.execSQL(line)
            } catch (e: SQLiteException) {
                Timber.w("@@@ Execute sql meet ${e.message}")
                continue
            }
        }
    }
}

private fun dumpTable(db: SQLiteDatabase, writer: BufferedWriter, tableName: String) {
    val cursor = db.rawQuery("SELECT sql FROM sqlite_master WHERE name=?", arrayOf(tableName))
    cursor.use { c ->
        if (c.moveToFirst()) {
            writer.write(c.getString(0))
            writer.write(";\n")
        } else {
            Timber.w("@@@ No such table($tableName)")
        }
    }
}

private fun dumpRows(db: SQLiteDatabase, writer: BufferedWriter, tableName: String) {
    val cursor = db.rawQuery("SELECT * FROM $tableName", null)
    cursor.use { c ->
        val columnCount = cursor.columnCount
        if (columnCount <= 0) return
        if (!c.moveToFirst()) return

        val builder = StringBuilder()
            .append("INSERT INTO '$tableName' (")
        val types = mutableListOf<Int>()
        for (i in 0 until columnCount) {
            if (i > 0) {
                builder.append(", ")
            }
            builder.append("'${c.getColumnName(i)}'")

            types.add(c.getType(i))
            Timber.d("@@@ type[$i]=${types[i]}")
        }
        builder.append(") VALUES (")
        val preInsert = builder.toString()

        while (c.moveToNext()) {
            writer.write(preInsert)

            for (i in 0 until columnCount) {
                if (i > 0) {
                    writer.write(", ")
                }
                try {
                    dumpColumns(writer, c, i, types[i])
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }

            writer.write(");\n")
        }
    }
}

private fun dumpColumns(writer: BufferedWriter, c: Cursor, col: Int, type: Int) {
    when (type) {
        Cursor.FIELD_TYPE_BLOB -> writer.write(dumpBlob(c.getBlob(col)))
        Cursor.FIELD_TYPE_INTEGER -> writer.write(c.getLong(col).toString())
        Cursor.FIELD_TYPE_FLOAT -> writer.write(c.getFloat(col).toString())
        else -> writer.write("'${c.getString(col)}'")
    }
}

private fun dumpBlob(blob: ByteArray): String {
    val builder = StringBuilder(blob.size * 2 + 3)
        .append("X'")
    for (b in blob) {
        builder.append(String.format("%02X", b and 0xff.toByte()))
    }
    builder.append('\'')
    return builder.toString()
}