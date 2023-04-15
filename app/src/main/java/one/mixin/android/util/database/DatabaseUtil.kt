package one.mixin.android.util.database

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.session.Session
import one.mixin.android.util.reportException
import timber.log.Timber
import java.io.File

@SuppressLint("ObsoleteSdkInt")
suspend fun clearJobs(context: Context) = withContext(Dispatchers.IO) {
    val supportsDeferForeignKeys = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    val dbFile = getDbFile(context, Constants.DataBase.PENDING_DB_NAME)
    if (!dbFile.exists()) {
        return@withContext
    }
    var db: SQLiteDatabase? = null
    try {
        db = SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE,
        )
        if (!supportsDeferForeignKeys) {
            db.execSQL("PRAGMA foreign_keys = FALSE")
        }
        if (supportsDeferForeignKeys) {
            db.execSQL("PRAGMA defer_foreign_keys = TRUE")
        }
        db.beginTransaction()
        db.execSQL("DELETE FROM `jobs`")
        db.setTransactionSuccessful()
    } catch (e: Exception) {
        reportException(e)
    } finally {
        db?.endTransaction()
        if (!supportsDeferForeignKeys) {
            db?.execSQL("PRAGMA foreign_keys = TRUE")
        }
        db?.rawQuery("PRAGMA wal_checkpoint(FULL)", null)?.close()
    }
}

fun getDbPath(context: Context, name: String, optional: Boolean = false): String {
    return getDbFile(context, name, optional).absolutePath
}

fun getDbFile(context: Context, name: String, optional: Boolean = false): File {
    if (optional && Session.getAccount() == null) {
        return context.getDatabasePath(name)
    }
    val identityNumber = requireNotNull(Session.getAccount()?.identityNumber)
    val toDir = File(context.getDatabasePath(Constants.DataBase.DB_NAME).parentFile, identityNumber)
    if (!toDir.exists()) toDir.mkdirs()
    return File(toDir, name)
}
