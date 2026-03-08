package one.mixin.android.util.database

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.moveTo
import one.mixin.android.session.Session
import one.mixin.android.util.reportException
import one.mixin.android.vo.Account
import timber.log.Timber
import java.io.File

private fun readMixinUserIdFromFile(dbFile: File): String? {
    if (!dbFile.exists() || dbFile.length() <= 0) {
        return null
    }
    var c: Cursor? = null
    var db: SQLiteDatabase? = null
    return try {
        db =
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
        c = db.rawQuery("SELECT user_id FROM users WHERE relationship = 'ME' LIMIT 1", null)
        if (c.moveToFirst()) {
            c.getString(0)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    } finally {
        c?.close()
        db?.close()
    }
}

private fun deleteDatabaseAndSidecars(dbFile: File) {
    dbFile.parent?.let {
        File("$it${File.separator}${dbFile.name}-shm").delete()
        File("$it${File.separator}${dbFile.name}-wal").delete()
        File("$it${File.separator}${dbFile.name}-journal").delete()
    }
    if (dbFile.exists()) {
        do {
            dbFile.delete()
        } while (dbFile.exists())
    }
}

private fun currentIdentityNumber(): String? = Session.getAccount()?.identityNumber?.takeIf { it.isNotBlank() }

suspend fun getLastUserId(context: Context): String? =
    withContext(Dispatchers.IO) {
        val candidates = LinkedHashSet<File>()
        candidates.add(legacyDatabaseFile(context))
        currentIdentityNumber()?.let { identityNumber ->
            val scopedCurrent = databaseFile(context, identityNumber)
            if (scopedCurrent != legacyDatabaseFile(context)) {
                candidates.add(scopedCurrent)
            }
        }
        val scopedRoot = File(context.filesDir.parent, "databases")
        scopedRoot.listFiles()
            ?.map { File(it, Constants.DataBase.DB_NAME) }
            ?.sortedByDescending { it.lastModified() }
            ?.forEach { candidates.add(it) }
        for (dbFile in candidates) {
            val userId = readMixinUserIdFromFile(dbFile)
            if (!userId.isNullOrBlank()) {
                return@withContext userId
            }
        }
        return@withContext null
    }

@SuppressLint("ObsoleteSdkInt")
suspend fun clearDatabase(context: Context) {
    try {
        clearFts(context)
        clearPending(context)
        val scopedDbFile = currentIdentityNumber()?.let { databaseFile(context, it) }
        val legacyDbFile = legacyDatabaseFile(context)
        scopedDbFile?.let { scoped ->
            deleteDatabaseAndSidecars(scoped)
        }
        if (scopedDbFile == null || legacyDbFile != scopedDbFile) {
            deleteDatabaseAndSidecars(legacyDbFile)
        }
    } catch (e: Exception) {
        Timber.e(e)
    }

    try {
        if (Build.VERSION.SDK_INT >= 28) {
            context.getDatabasePath(Constants.DataBase.DB_NAME).delete()
            context.getDatabasePath(Constants.DataBase.DB_NAME + "-shm").delete()
            context.getDatabasePath(Constants.DataBase.DB_NAME + "-wal").delete()
        } else {
            SQLiteDatabase.deleteDatabase(context.getDatabasePath(Constants.DataBase.DB_NAME))
        }
    } catch (e: Exception) {
        reportException(e)
    }
}

@SuppressLint("ObsoleteSdkInt")
suspend fun clearPending(context: Context) =
    withContext(Dispatchers.IO) {
        val scopedDbFile = currentIdentityNumber()?.let { pendingDatabaseFile(context, it) }
        val legacyDbFile = legacyPendingDatabaseFile(context)
        scopedDbFile?.let { scoped ->
            deleteDatabaseAndSidecars(scoped)
        }
        if (scopedDbFile == null || legacyDbFile != scopedDbFile) {
            deleteDatabaseAndSidecars(legacyDbFile)
        }
    }

suspend fun clearFts(context: Context) =
    withContext(Dispatchers.IO) {
        val scopedDbFile = currentIdentityNumber()?.let { ftsDatabaseFile(context, it) }
        val legacyDbFile = legacyFtsDatabaseFile(context)
        scopedDbFile?.let { scoped ->
            deleteDatabaseAndSidecars(scoped)
        }
        if (scopedDbFile == null || legacyDbFile != scopedDbFile) {
            deleteDatabaseAndSidecars(legacyDbFile)
        }
    }

@SuppressLint("ObsoleteSdkInt")
suspend fun clearJobsAndRawTransaction(
    context: Context,
    identityNumber: String,
) {
        val supportsDeferForeignKeys = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        val scopedDbFile = databaseFile(context, identityNumber)
        val legacyDbFile = legacyDatabaseFile(context)
        if (!scopedDbFile.exists() && !legacyDbFile.exists()) {
            return
        }
        var db: SupportSQLiteDatabase? = null
        try {
            // Init database
            MixinDatabase.getDatabase(context, identityNumber)
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

fun dbDir(
    context: Context,
    identityNumber: String,
): File {
    require(identityNumber.isNotBlank()) { "identityNumber is required for database directory." }
    val baseDir = File(context.filesDir.parent, "databases")
    val dir = File(baseDir, identityNumber)
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
    return context.getDatabasePath(Constants.DataBase.DB_NAME)
}

fun databaseFile(
    context: Context,
    identityNumber: String,
): File {
    return File(dbDir(context, identityNumber), Constants.DataBase.DB_NAME)
}

fun legacyPendingDatabaseFile(context: Context): File {
    return context.getDatabasePath(Constants.DataBase.PENDING_DB_NAME)
}

fun pendingDatabaseFile(
    context: Context,
    identityNumber: String,
): File {
    return File(dbDir(context, identityNumber), Constants.DataBase.PENDING_DB_NAME)
}

fun legacyFtsDatabaseFile(context: Context): File {
    return context.getDatabasePath(Constants.DataBase.FTS_DB_NAME)
}

fun ftsDatabaseFile(
    context: Context,
    identityNumber: String,
): File {
    return File(dbDir(context, identityNumber), Constants.DataBase.FTS_DB_NAME)
}

private fun moveSidecarIfExists(
    fromDir: File,
    toDir: File,
    databaseName: String,
    suffix: String,
) {
    val fromFile = File(fromDir, databaseName + suffix)
    if (!fromFile.exists()) {
        return
    }
    if (!toDir.exists()) {
        toDir.mkdirs()
    }
    val toFile = File(toDir, databaseName + suffix)
    fromFile.moveTo(toFile)
}

private fun deleteSidecarIfExists(
    dir: File,
    databaseName: String,
    suffix: String,
) {
    val sidecar = File(dir, databaseName + suffix)
    if (sidecar.exists()) {
        sidecar.delete()
    }
}

fun moveLegacyDatabaseFile(context: Context, account: Account): Boolean {
    val dbFile = legacyDatabaseFile(context)
    if (!dbFile.exists() || dbFile.length() <= 0) {
        return true
    }
    val targetDb = databaseFile(context, account.identityNumber)
    if (targetDb.exists() && targetDb.length() > 0) {
        return true
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
        if (account.userId != userId) {
            return false
        }
        val dir = dbDir(context, account.identityNumber)
        if (!dir.exists()) dir.mkdirs()
        c?.close()
        c = null
        db?.close()
        db = null
        dbFile.moveTo(targetDb)
        val fromDir = dbFile.parentFile
        if (fromDir != null) {
            moveSidecarIfExists(fromDir, dir, Constants.DataBase.DB_NAME, "-wal")
            moveSidecarIfExists(fromDir, dir, Constants.DataBase.DB_NAME, "-shm")
            moveSidecarIfExists(fromDir, dir, Constants.DataBase.DB_NAME, "-journal")
        }
        return true
    } catch (e: Exception) {
        Timber.e(e)
        return false
    } finally {
        c?.close()
        db?.close()
    }
}

fun moveLegacyPendingDatabaseFile(context: Context, account: Account) {
    val dbFile = legacyPendingDatabaseFile(context)
    if (!dbFile.exists() || dbFile.length() <= 0) {
        return
    }
    val targetDb = pendingDatabaseFile(context, account.identityNumber)
    if (targetDb.exists() && targetDb.length() > 0) {
        return
    }
    var c: Cursor? = null
    var db: SQLiteDatabase? = null
    try {
        var userId: String? = null
        db =
            SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
        c = db.rawQuery("SELECT user_id FROM jobs WHERE user_id IS NOT NULL LIMIT 1", null)
        if (c.moveToFirst()) {
            userId = c.getString(0)
        }
        if (!userId.isNullOrBlank() && account.userId != userId) {
            dbFile.delete()
            dbFile.parentFile?.let { fromDir ->
                deleteSidecarIfExists(fromDir, Constants.DataBase.PENDING_DB_NAME, "-wal")
                deleteSidecarIfExists(fromDir, Constants.DataBase.PENDING_DB_NAME, "-shm")
                deleteSidecarIfExists(fromDir, Constants.DataBase.PENDING_DB_NAME, "-journal")
            }
            return
        }
        val targetDir = targetDb.parentFile ?: return
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        c?.close()
        c = null
        db?.close()
        db = null
        dbFile.moveTo(targetDb)
        val fromDir = dbFile.parentFile
        if (fromDir != null) {
            moveSidecarIfExists(fromDir, targetDir, Constants.DataBase.PENDING_DB_NAME, "-wal")
            moveSidecarIfExists(fromDir, targetDir, Constants.DataBase.PENDING_DB_NAME, "-shm")
            moveSidecarIfExists(fromDir, targetDir, Constants.DataBase.PENDING_DB_NAME, "-journal")
        }
    } catch (e: Exception) {
        Timber.e(e)
    } finally {
        c?.close()
        db?.close()
    }
}

fun moveLegacyFtsDatabaseFile(context: Context, account: Account) {
    val dbFile = legacyFtsDatabaseFile(context)
    if (!dbFile.exists() || dbFile.length() <= 0) {
        return
    }
    val targetDb = ftsDatabaseFile(context, account.identityNumber)
    if (targetDb.exists() && targetDb.length() > 0) {
        return
    }
    val targetDir = targetDb.parentFile ?: return
    if (!targetDir.exists()) {
        targetDir.mkdirs()
    }
    dbFile.moveTo(targetDb)
    val fromDir = dbFile.parentFile
    if (fromDir != null) {
        moveSidecarIfExists(fromDir, targetDir, Constants.DataBase.FTS_DB_NAME, "-wal")
        moveSidecarIfExists(fromDir, targetDir, Constants.DataBase.FTS_DB_NAME, "-shm")
        moveSidecarIfExists(fromDir, targetDir, Constants.DataBase.FTS_DB_NAME, "-journal")
    }
}
