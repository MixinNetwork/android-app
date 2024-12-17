package one.mixin.android.util.database

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.db.MixinDatabase
import one.mixin.android.util.reportException
import timber.log.Timber
import java.io.File

suspend fun getLastUserId(context: Context): String? =
    withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(Constants.DataBase.DB_NAME)
        if (!dbFile.exists()) {
            return@withContext null
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
            return@withContext userId
        } catch (e: Exception) {
            return@withContext null
        } finally {
            c?.close()
            db?.close()
        }
    }

@SuppressLint("ObsoleteSdkInt")
suspend fun clearDatabase(context: Context) {
        try {
            clearFts(context)
            clearPending(context)
            val dbFile = context.getDatabasePath(Constants.DataBase.DB_NAME)
            if (!dbFile.exists()) {
                return
            }
            dbFile.parent?.let {
                File("$it${File.separator}${Constants.DataBase.DB_NAME}-shm").delete()
                File("$it${File.separator}${Constants.DataBase.DB_NAME}-wal").delete()
                File("$it${File.separator}${Constants.DataBase.DB_NAME}-journal").delete()
            }
            do {
                dbFile.delete()
            } while (dbFile.exists())
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

@SuppressLint("ObsoleteSdkInt")
suspend fun clearPending(context: Context) =
    withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(Constants.DataBase.PENDING_DB_NAME)
        if (!dbFile.exists()) {
            return@withContext
        }
        dbFile.parent?.let {
            File("$it${File.separator}${Constants.DataBase.PENDING_DB_NAME}-shm").delete()
            File("$it${File.separator}${Constants.DataBase.PENDING_DB_NAME}-wal").delete()
            File("$it${File.separator}${Constants.DataBase.PENDING_DB_NAME}-journal").delete()
        }
        dbFile.delete()
    }

suspend fun clearFts(context: Context) =
    withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(Constants.DataBase.FTS_DB_NAME)
        if (!dbFile.exists()) {
            return@withContext
        }
        dbFile.parent?.let {
            File("$it${File.separator}${Constants.DataBase.FTS_DB_NAME}-shm").delete()
            File("$it${File.separator}${Constants.DataBase.FTS_DB_NAME}-wal").delete()
            File("$it${File.separator}${Constants.DataBase.FTS_DB_NAME}-journal").delete()
        }
        dbFile.delete()
    }

@SuppressLint("ObsoleteSdkInt")
suspend fun clearJobsAndRawTransaction(context: Context) {
        val supportsDeferForeignKeys = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        val dbFile = context.getDatabasePath(Constants.DataBase.DB_NAME)
        if (!dbFile.exists()) {
            return
        }
        var db: SupportSQLiteDatabase? = null
        try {
            // Init database
            MixinDatabase.getDatabase(context)
            db = MixinDatabase.getWritableDatabase() ?: return
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
            Timber.e("Clear jobs and raw transaction")
        } catch (e: Exception) {
            Timber.e(e)
            reportException(e)
        } finally {
            db?.endTransaction()
            if (!supportsDeferForeignKeys) {
                db?.execSQL("PRAGMA foreign_keys = TRUE")
            }
        }
}
