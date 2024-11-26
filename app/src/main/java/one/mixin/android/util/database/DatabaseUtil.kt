package one.mixin.android.util.database

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.extension.moveTo
import one.mixin.android.session.Session
import one.mixin.android.util.reportException
import java.io.File

@SuppressLint("ObsoleteSdkInt")
suspend fun clearJobsAndRawTransaction(context: Context, identityNumber: String) =
    withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, identityNumber)
        val supportsDeferForeignKeys = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        val dbFile = File(dir, Constants.DataBase.DB_NAME)
        if (!dbFile.exists()) {
            return@withContext
        }
        var db: SQLiteDatabase? = null
        try {
            db =
                SQLiteDatabase.openDatabase(
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
            db.execSQL("DELETE FROM `raw_transactions`")
            db.execSQL("DELETE FROM `outputs`")
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

fun legacyDatabaseExists(context: Context): Boolean {
    val dbFile = context.getDatabasePath(Constants.DataBase.DB_NAME)
    return dbFile.exists() && dbFile.length() > 0
}

fun migrationDbFile(context: Context) {
    if (Session.getAccount() == null) {
        return
    }

    val dbFile = context.getDatabasePath(Constants.DataBase.DB_NAME)
    if (!dbFile.exists() || dbFile.length() <= 0) {
        return
    }
    val dir = File(dbFile.parent, Session.getAccount()!!.identityNumber)
    if (!dir.exists()) dir.mkdirs()
    checkpoint(dbFile)
    moveDbFile(dbFile, dir)
    dbFile.parent?.let {
        checkpoint(File("$it${File.separator}${Constants.DataBase.FTS_DB_NAME}"))
        checkpoint(File("$it${File.separator}${Constants.DataBase.PENDING_DB_NAME}"))

        moveDbFile(
            File("$it${File.separator}${Constants.DataBase.DB_NAME}-shm"), dir
        )
        moveDbFile(
            File("$it${File.separator}${Constants.DataBase.DB_NAME}-wal"), dir
        )
        moveDbFile(
            File("$it${File.separator}${Constants.DataBase.DB_NAME}-journal"), dir
        )
        moveDbFile(
            File("$it${File.separator}${Constants.DataBase.FTS_DB_NAME}"), dir
        )
        moveDbFile(
            File("$it${File.separator}${Constants.DataBase.FTS_DB_NAME}-shm"), dir
        )
        moveDbFile(
            File("$it${File.separator}${Constants.DataBase.FTS_DB_NAME}-wal"), dir
        )
        moveDbFile(
            File("$it${File.separator}${Constants.DataBase.FTS_DB_NAME}-journal"), dir
        )
        moveDbFile(
            File("$it${File.separator}${Constants.DataBase.PENDING_DB_NAME}"), dir
        )
        moveDbFile(
            File("$it${File.separator}${Constants.DataBase.PENDING_DB_NAME}-shm"), dir
        )
        moveDbFile(
            File("$it${File.separator}${Constants.DataBase.PENDING_DB_NAME}-wal"), dir
        )
        moveDbFile(
            File("$it${File.separator}${Constants.DataBase.PENDING_DB_NAME}-journal"), dir
        )
    }
}

private fun checkpoint(dbFile: File){
    if (!dbFile.exists()) return
    var db: SQLiteDatabase? = null
    try {
        db =
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE,
            )
        db?.rawQuery("PRAGMA wal_checkpoint(FULL)", null)?.close()
    } catch (e: Exception) {
        reportException(e)
    }
}

private fun moveDbFile(file: File, dir: File) {
    if (!file.exists() || file.length() <= 0) {
        return
    }
    file.moveTo(File(dir, file.name))
}