package one.mixin.android.util.backup

import android.Manifest
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.StatFs
import androidx.annotation.RequiresPermission
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.Constants.DataBase.DB_NAME
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.runInTransaction
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getLegacyBackupPath
import one.mixin.android.extension.getMediaPath
import one.mixin.android.extension.getOldBackupPath
import one.mixin.android.util.PropertyHelper
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.coroutines.CoroutineContext

private const val BACKUP_POSTFIX = ".backup"

@RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
suspend fun backup(
    context: Context,
    callback: (Result) -> Unit
) = coroutineScope {
    val dbFile = context.getDatabasePath(DB_NAME)
    if (dbFile == null) {
        withContext(Dispatchers.Main) {
            callback(Result.NOT_FOUND)
        }
        return@coroutineScope
    }

    val backupDir = context.getLegacyBackupPath(true) ?: return@coroutineScope
    val availableSize = StatFs(backupDir.path).availableBytes
    if (availableSize < dbFile.length()) {
        withContext(Dispatchers.Main) {
            callback(Result.NO_AVAILABLE_MEMORY)
        }
        return@coroutineScope
    }

    val exists = backupDir.listFiles()?.any { it.name.contains(dbFile.name) } == true
    val name = dbFile.name
    val tmpName = if (exists) {
        "$name$BACKUP_POSTFIX"
    } else {
        name
    }
    val copyPath = "$backupDir${File.separator}$tmpName"
    var result: File? = null
    try {
        runInTransaction {
            MixinDatabase.checkPoint()
            result = dbFile.copyTo(File(copyPath), overwrite = true)
        }
        context.getOldBackupPath(false)?.deleteRecursively()
    } catch (e: Exception) {
        result?.delete()
        withContext(Dispatchers.Main) {
            callback(Result.FAILURE)
        }
        return@coroutineScope
    }

    var db: SQLiteDatabase? = null
    try {
        db = SQLiteDatabase.openDatabase(
            "$backupDir${File.separator}$tmpName",
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
    } catch (e: Exception) {
        result?.delete()
        db?.close()
        withContext(Dispatchers.Main) {
            callback(Result.FAILURE)
        }
        return@coroutineScope
    }
    try {
        db.execSQL("UPDATE participant_session SET sent_to_server = NULL")
        db.execSQL("DELETE FROM jobs")
        db.execSQL("DELETE FROM flood_messages")
        db.execSQL("DELETE FROM offsets")
    } catch (ignored: Exception) {
    } finally {
        db.close()
    }

    try {
        backupDir.listFiles()?.forEach { f ->
            if (f.name != tmpName) {
                f.delete()
            }
        }
        if (tmpName.contains(BACKUP_POSTFIX)) {
            result?.renameTo(File("$backupDir${File.separator}$name"))
        }
    } catch (e: Exception) {
        result?.delete()
        withContext(Dispatchers.Main) {
            callback(Result.FAILURE)
        }
        return@coroutineScope
    }

    withContext(Dispatchers.Main) {
        callback(Result.SUCCESS)
    }
}

@RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
suspend fun restore(
    context: Context,
    callback: (Result) -> Unit
) = withContext(Dispatchers.IO) {
    val target = findBackup(context, coroutineContext)
        ?: return@withContext callback(Result.NOT_FOUND)
    val file = context.getDatabasePath(DB_NAME)
    try {
        if (file.exists()) {
            file.delete()
        }
        File("${file.absolutePath}-wal").delete()
        File("${file.absolutePath}-shm").delete()
        target.copyTo(file)

        // Reset BACKUP_LAST_TIME so that the user who restores the backup does not need to backup after login
        PropertyHelper.updateKeyValue(
            Constants.BackUp.BACKUP_LAST_TIME,
            System.currentTimeMillis().toString()
        )

        withContext(Dispatchers.Main) {
            callback(Result.SUCCESS)
        }
    } catch (e: Exception) {
        callback(Result.FAILURE)
    }
}

suspend fun restoreApi29(
    context: Context,
    callback: (Result) -> Unit
) = withContext(Dispatchers.IO) {
    val backupDirectoryUri =
        context.defaultSharedPreferences.getString(
            Constants.Account.PREF_BACKUP_DIRECTORY,
            null
        )?.toUri()
    if (backupDirectoryUri == null) {
        withContext(Dispatchers.Main) {
            callback(Result.NOT_FOUND)
        }
        return@withContext
    }
    val backupDirectory =
        DocumentFile.fromTreeUri(context, backupDirectoryUri)
    if (backupDirectory == null || !backupDirectory.exists()) {
        withContext(Dispatchers.Main) {
            callback(Result.NOT_FOUND)
        }
        return@withContext
    }
    val backupDb = backupDirectory.findFile(DB_NAME)
    if (backupDb == null || !backupDb.exists()) {

        withContext(Dispatchers.Main) {
            callback(Result.NOT_FOUND)
        }
        return@withContext
    }
    val file = context.getDatabasePath(DB_NAME)
    try {
        val inputStream = context.contentResolver.openInputStream(backupDb.uri)
        if (inputStream == null) {
            withContext(Dispatchers.Main) {
                callback(Result.NOT_FOUND)
            }
            return@withContext
        }
        if (file.exists()) {
            file.delete()
        }
        File("${file.absolutePath}-wal").delete()
        File("${file.absolutePath}-shm").delete()
        FileOutputStream(file).use { output ->
            inputStream.copyTo(output)
        }
        val backupMediaDirectory = backupDirectory.findFile("Media")
        val mediaPath = context.getMediaPath()
        if (mediaPath != null && backupMediaDirectory != null && backupMediaDirectory.exists() && backupMediaDirectory.isDirectory) {
            backupMediaDirectory.listFiles().forEach {
                copyFileToDirectory(context, it, mediaPath)
            }
        }

        // Reset BACKUP_LAST_TIME so that the user who restores the backup does not need to backup after login
        PropertyHelper.updateKeyValue(
            Constants.BackUp.BACKUP_LAST_TIME,
            System.currentTimeMillis().toString()
        )

        withContext(Dispatchers.Main) {
            callback(Result.SUCCESS)
        }
    } catch (e: Exception) {
        Timber.e(e)
        withContext(Dispatchers.Main) {
            callback(Result.FAILURE)
        }
    }
}

@RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
suspend fun delete(
    context: Context
): Boolean = withContext(Dispatchers.IO) {
    val backupDir = context.getLegacyBackupPath()
    return@withContext backupDir?.deleteRecursively() ?: return@withContext false
}

@RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
suspend fun findBackup(
    context: Context,
    coroutineContext: CoroutineContext
): File? = findNewBackup(context, coroutineContext) ?: findNewBackup(
    context,
    coroutineContext,
    legacy = true
) ?: findOldBackup(context, coroutineContext)

@RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
suspend fun findNewBackup(
    context: Context,
    coroutineContext: CoroutineContext,
    legacy: Boolean = false
): File? = withContext(coroutineContext) {
    val backupDir = context.getLegacyBackupPath(legacy = legacy) ?: return@withContext null
    if (!backupDir.exists() || !backupDir.isDirectory) return@withContext null
    if (checkDb("$backupDir${File.separator}$DB_NAME")) {
        return@withContext File("$backupDir${File.separator}$DB_NAME")
    }
    null
}

@RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
suspend fun findOldBackup(
    context: Context,
    coroutineContext: CoroutineContext
) = withContext(coroutineContext) {
    val backupDir = context.getOldBackupPath() ?: return@withContext null
    if (!backupDir.exists() || !backupDir.isDirectory) return@withContext null
    val files = backupDir.listFiles()
    if (files.isNullOrEmpty()) return@withContext null
    files.forEach { f ->
        val name = f.name
        val exists = try {
            val version = name.split('.')[2].toInt()
            version in Constants.DataBase.MINI_VERSION..Constants.DataBase.CURRENT_VERSION
        } catch (e: Exception) {
            false
        }
        if (exists) return@withContext f
    }
    null
}

fun findOldBackupSync(context: Context, legacy: Boolean = false): File? {
    val backupDir = context.getOldBackupPath(legacy = legacy) ?: return null
    if (!backupDir.exists() || !backupDir.isDirectory) return null
    val files = backupDir.listFiles()
    if (files.isNullOrEmpty()) return null
    files.forEach { f ->
        val name = f.name
        val exists = try {
            val version = name.split('.')[2].toInt()
            version in Constants.DataBase.MINI_VERSION..Constants.DataBase.CURRENT_VERSION
        } catch (e: Exception) {
            false
        }
        if (exists) return f
    }
    return null
}

fun checkDb(path: String): Boolean {
    var db: SQLiteDatabase? = null
    return try {
        db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
        db.version in Constants.DataBase.MINI_VERSION..Constants.DataBase.CURRENT_VERSION
    } catch (ignored: Exception) {
        false
    } finally {
        db?.close()
    }
}

fun canUserAccessBackupDirectory(context: Context): Boolean {
    val backupDirectoryUri =
        context.defaultSharedPreferences.getString(Constants.Account.PREF_BACKUP_DIRECTORY, null)
            ?.toUri()
            ?: return false
    return internalCheckAccessBackupDirectory(context, backupDirectoryUri)
}

private fun internalCheckAccessBackupDirectory(context: Context, uri: Uri): Boolean {
    val backupDirectory = DocumentFile.fromTreeUri(context, uri)
    return backupDirectory != null && backupDirectory.canRead() && backupDirectory.canWrite()
}

suspend fun backupApi29(context: Context, callback: (Result) -> Unit) =
    withContext(Dispatchers.IO) {
        val backupDirectoryUri =
            context.defaultSharedPreferences.getString(
                Constants.Account.PREF_BACKUP_DIRECTORY,
                null
            )
                ?.toUri()
                ?: return@withContext
        val backupDirectory =
            DocumentFile.fromTreeUri(context, backupDirectoryUri) ?: return@withContext
        if (!internalCheckAccessBackupDirectory(context, backupDirectoryUri)) {
            return@withContext
        }
        backupDirectory.findFile("mixin.db")?.delete()
        val file = backupDirectory.createFile("application/octet-stream", DB_NAME) ?: return@withContext
        val outputStream = context.contentResolver.openOutputStream(file.uri) ?: return@withContext
        val dbFile = context.getDatabasePath(DB_NAME)
        if (dbFile == null) {
            withContext(Dispatchers.Main) {
                callback(Result.NOT_FOUND)
            }
            return@withContext
        }
        val inputStream = FileInputStream(dbFile)
        try {
            file.uri.copyFromInputStream(inputStream)
            context.getMediaPath()?.let {
                copyFileToDirectory(it, backupDirectory)
            }
            withContext(Dispatchers.Main) {
                callback.invoke(Result.SUCCESS)
            }
        } catch (e: Exception) {
            Timber.e(e)
            withContext(Dispatchers.Main) {
                callback.invoke(Result.FAILURE)
            }
        } finally {
            inputStream.close()
            outputStream.close()
        }
    }

private fun copyFileToDirectory(file: File, dir: DocumentFile) {
    if (file.isDirectory) {
        val childDir = dir.createDirectory(file.name) ?: return
        file.listFiles()?.forEach { f ->
            copyFileToDirectory(f, childDir)
        }
    } else {
        val documentFile = dir.createFile("application/octet-stream", file.name) ?: return
        val inputStream = FileInputStream(file)
        documentFile.uri.copyFromInputStream(inputStream)
    }
}

private fun copyFileToDirectory(context: Context, file: DocumentFile, dir: File) {
    val name = file.name ?: return
    val f = File(dir, name)
    if (file.isDirectory) {
        file.listFiles().forEach {
            copyFileToDirectory(context, it, f)
        }
    } else {
        Timber.e("copy ${file.name} ${dir.absolutePath}")
        val outputStream = context.contentResolver.openOutputStream(file.uri) ?: return
        val inputStream = FileInputStream(f)
        outputStream.use { output ->
            inputStream.copyTo(output)
        }
    }
}
