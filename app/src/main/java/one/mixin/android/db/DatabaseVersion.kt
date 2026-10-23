package one.mixin.android.db

import android.database.sqlite.SQLiteDatabase
import java.io.File

fun readVersion(file: File): Int {
    return SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
        db.version
    }
}
