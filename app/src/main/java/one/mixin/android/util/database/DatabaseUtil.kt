package one.mixin.android.util.database

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.DataBase.DB_NAME
import one.mixin.android.Constants.DataBase.PENDING_DB_NAME
import one.mixin.android.Constants.DataBase.FTS_DB_NAME
import one.mixin.android.extension.moveTo
import one.mixin.android.session.Session
import one.mixin.android.util.reportException
import one.mixin.android.vo.Account
import timber.log.Timber
import java.io.File

@SuppressLint("ObsoleteSdkInt")
suspend fun clearJobsAndRawTransaction(context: Context, identityNumber: String) =
    withContext(Dispatchers.IO) {
        val dir = dbDir(context)
        val supportsDeferForeignKeys = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        val dbFile = File(dir, DB_NAME)
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

fun migrationDbFile(context: Context) {
    if (Session.getAccount() == null) {
        return
    }

    val dbFile = legacyDatabaseFile(context)
    if (!dbFile.exists() || dbFile.length() <= 0) {
        return
    }
    val dir = dbDir(context)
    if (!dir.exists()) dir.mkdirs()
    checkpoint(dbFile)
    moveDbFile(dbFile, dir)
    dbFile.parent?.let {
        checkpoint(File("$it${File.separator}${FTS_DB_NAME}"))
        checkpoint(File("$it${File.separator}${PENDING_DB_NAME}"))

        moveDbFile(
            File("$it${File.separator}${DB_NAME}-shm"), dir
        )
        moveDbFile(
            File("$it${File.separator}${DB_NAME}-wal"), dir
        )
        moveDbFile(
            File("$it${File.separator}${DB_NAME}-journal"), dir
        )
        moveDbFile(
            File("$it${File.separator}${FTS_DB_NAME}"), dir
        )
        moveDbFile(
            File("$it${File.separator}${FTS_DB_NAME}-shm"), dir
        )
        moveDbFile(
            File("$it${File.separator}${FTS_DB_NAME}-wal"), dir
        )
        moveDbFile(
            File("$it${File.separator}${FTS_DB_NAME}-journal"), dir
        )
        moveDbFile(
            File("$it${File.separator}${PENDING_DB_NAME}"), dir
        )
        moveDbFile(
            File("$it${File.separator}${PENDING_DB_NAME}-shm"), dir
        )
        moveDbFile(
            File("$it${File.separator}${PENDING_DB_NAME}-wal"), dir
        )
        moveDbFile(
            File("$it${File.separator}${PENDING_DB_NAME}-journal"), dir
        )
    }
}

private fun checkpoint(dbFile: File) {
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

fun dbDir(context: Context): File {
    val baseDir = File(context.filesDir.parent, "database")
    val dir = File(baseDir, Session.getAccount()?.identityNumber ?: "temp")
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return dir
}

fun legacyDatabaseExists(context: Context): Boolean {
    val dbFile = legacyDatabaseFile(context)
    return dbFile.exists() && dbFile.length() > 0
}

fun legacyDatabaseFile(context: Context): File {
    return context.getDatabasePath(DB_NAME)
}

fun databaseFile(context: Context): File {
    return File(dbDir(context), DB_NAME)
}

fun moveLegacyDatabaseFile(context: Context, account: Account) {
    val dbFile = legacyDatabaseFile(context)
    if (!dbFile.exists() || dbFile.length() <= 0) {
        return
    }
    var c: Cursor? = null
    var db: SQLiteDatabase? = null
    try {
        db =
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
        c = db.rawQuery("SELECT user_id FROM users WHERE relationship = 'ME'", null)
        var userId: String? = null
        if (c.moveToFirst()) {
            userId = c.getString(0)
        }
        if (account.userId == userId){
            val dir = dbDir(context)
            if (!dir.exists()) dir.mkdirs()
            c?.close()
            c = null
            db?.close()
            db = null
            dbFile.moveTo(databaseFile(context))
        }
    } catch (e: Exception) {
        Timber.e(e)
    } finally {
        c?.close()
        db?.close()
    }
}