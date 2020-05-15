package one.mixin.android.util.database

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import one.mixin.android.Constants

suspend fun getLastUserId(context: Context): String? {
    val dbFile = context.getDatabasePath(Constants.DataBase.DB_NAME)
    if (!dbFile.exists()) {
        return null
    }
    var c: Cursor? = null
    var db: SQLiteDatabase? = null

    try {
        db = SQLiteDatabase.openDatabase(
            dbFile.absolutePath, null,
            SQLiteDatabase.OPEN_READONLY
        )
        c = db.rawQuery("SELECT user_id FROM users WHERE relationship = 'ME'", null)
        var userId: String? = null
        if (c.moveToFirst()) {
            userId = c.getString(0)
        }
        return userId
    } catch (e: Exception) {
        return null
    } finally {
        c?.close()
        db?.close()
    }
}
