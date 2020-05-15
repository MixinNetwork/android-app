package one.mixin.android.util.database

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.Constants

suspend fun getLastUserId(context: Context): String? = withContext(Dispatchers.IO) {
    val dbFile = context.getDatabasePath(Constants.DataBase.DB_NAME)
    if (!dbFile.exists()) {
        return@withContext null
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
        return@withContext userId
    } catch (e: Exception) {
        return@withContext null
    } finally {
        c?.close()
        db?.close()
    }
}
