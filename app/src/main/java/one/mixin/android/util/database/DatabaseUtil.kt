package one.mixin.android.util.database

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.util.reportException

suspend fun getLastUserId(context: Context): String? = withContext(Dispatchers.IO) {
    val dbFile = context.getDatabasePath(Constants.DataBase.DB_NAME)
    if (!dbFile.exists()) {
        return@withContext null
    }
    var c: Cursor? = null
    var db: SQLiteDatabase? = null

    try {
        db = SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
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

@SuppressLint("ObsoleteSdkInt")
suspend fun clearDatabase(context: Context) = withContext(Dispatchers.IO) {
    val supportsDeferForeignKeys = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    val dbFile = context.getDatabasePath(Constants.DataBase.DB_NAME)
    if (!dbFile.exists()) {
        return@withContext
    }
    var db: SQLiteDatabase? = null
    try {
        db = SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
        if (!supportsDeferForeignKeys) {
            db.execSQL("PRAGMA foreign_keys = FALSE")
        }
        if (supportsDeferForeignKeys) {
            db.execSQL("PRAGMA defer_foreign_keys = TRUE")
        }
        db.beginTransaction()
        db.execSQL("DELETE FROM `users`")
        db.execSQL("DELETE FROM `conversations`")
        db.execSQL("DELETE FROM `messages`")
        db.execSQL("DELETE FROM `participants`")
        db.execSQL("DELETE FROM `participant_session`")
        db.execSQL("DELETE FROM `offsets`")
        db.execSQL("DELETE FROM `assets`")
        db.execSQL("DELETE FROM `assets_extra`")
        db.execSQL("DELETE FROM `snapshots`")
        db.execSQL("DELETE FROM `messages_history`")
        db.execSQL("DELETE FROM `sent_sender_keys`")
        db.execSQL("DELETE FROM `stickers`")
        db.execSQL("DELETE FROM `sticker_albums`")
        db.execSQL("DELETE FROM `apps`")
        db.execSQL("DELETE FROM `hyperlinks`")
        db.execSQL("DELETE FROM `flood_messages`")
        db.execSQL("DELETE FROM `addresses`")
        db.execSQL("DELETE FROM `resend_messages`")
        db.execSQL("DELETE FROM `resend_session_messages`")
        db.execSQL("DELETE FROM `sticker_relationships`")
        db.execSQL("DELETE FROM `top_assets`")
        db.execSQL("DELETE FROM `favorite_apps`")
        db.execSQL("DELETE FROM `jobs`")
        db.execSQL("DELETE FROM `message_mentions`")
        db.execSQL("DELETE FROM `messages_fts4`")
        db.execSQL("DELETE FROM `circles`")
        db.execSQL("DELETE FROM `circle_conversations`")
        db.execSQL("DELETE FROM `traces`")
        db.execSQL("DELETE FROM `transcript_messages`")
        db.execSQL("DELETE FROM `pin_messages`")
        db.execSQL("DELETE FROM `properties`")
        db.execSQL("DELETE FROM `remote_messages_status`")
        db.setTransactionSuccessful()
    } catch (e: Exception) {
        reportException(e)
    } finally {
        db?.endTransaction()
        if (!supportsDeferForeignKeys) {
            db?.execSQL("PRAGMA foreign_keys = TRUE")
        }
        db?.rawQuery("PRAGMA wal_checkpoint(FULL)", null)?.close()
        if (db?.inTransaction() == false) {
            db.execSQL("VACUUM")
        }
    }
}

@SuppressLint("ObsoleteSdkInt")
suspend fun clearJobs(context: Context) = withContext(Dispatchers.IO) {
    val supportsDeferForeignKeys = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    val dbFile = context.getDatabasePath(Constants.DataBase.DB_NAME)
    if (!dbFile.exists()) {
        return@withContext
    }
    var db: SQLiteDatabase? = null
    try {
        db = SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE
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
