package one.mixin.android.util.backup

import android.Manifest
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.StatFs
import androidx.annotation.RequiresPermission
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.withTransaction
import one.mixin.android.extension.getBackupPath

private const val BACKUP_POSTFIX = ".backup"

@RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
fun backup(
    context: Context,
    callback: (Result) -> Unit
) {
    val dbFile = context.getDatabasePath(Constants.DataBase.DB_NAME)
        ?: return callback(Result.NOT_FOUND)
    GlobalScope.launch(Dispatchers.IO) {
        val backupDir = context.getBackupPath()

        val availableSize = StatFs(backupDir.path).availableBytes
        if (availableSize < dbFile.length()) {
            withContext(Dispatchers.Main) {
                callback(Result.NO_AVAILABLE_MEMORY)
            }
            return@launch
        }

        val exists = backupDir.listFiles().any { it.name.contains(dbFile.name) }
        val name = "${dbFile.name}.${Constants.DataBase.CURRENT_VERSION}"
        val tmpName = if (exists) {
            "$name$BACKUP_POSTFIX"
        } else {
            name
        }
        val copyPath = "$backupDir${File.separator}$tmpName"
        var result: File? = null
        try {
            withTransaction {
                MixinDatabase.checkPoint()
                result = dbFile.copyTo(File(copyPath), overwrite = true)
            }
        } catch (e: Exception) {
            result?.delete()
            withContext(Dispatchers.Main) {
                callback(Result.FAILURE)
            }
            return@launch
        }

        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase("$backupDir${File.separator}$tmpName", null, SQLiteDatabase.OPEN_READWRITE)
        } catch (e: Exception) {
            result?.delete()
            db?.close()
            withContext(Dispatchers.Main) {
                callback(Result.FAILURE)
            }
            return@launch
        }
        try {
            db.execSQL("DELETE FROM sent_sender_keys")
            db.execSQL("DELETE FROM jobs")
            db.execSQL("DELETE FROM flood_messages")
            db.execSQL("DELETE FROM offsets")
        } catch (ignored: Exception) {
        } finally {
            db.close()
        }

        try {
            backupDir.listFiles().forEach { f ->
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
            return@launch
        }

        withContext(Dispatchers.Main) {
            callback(Result.SUCCESS)
        }
    }
}

@RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
fun restore(
    context: Context,
    callback: (Result) -> Unit
) {
    GlobalScope.launch {
        val target = findBackup(context, coroutineContext)
            ?: return@launch callback(Result.NOT_FOUND)
        val file = context.getDatabasePath(Constants.DataBase.DB_NAME)
        try {
            if (file.exists()) {
                file.delete()
            }
            File("${file.absolutePath}-wal").delete()
            File("${file.absolutePath}-shm").delete()
            target.copyTo(file)

            withContext(Dispatchers.Main) {
                callback(Result.SUCCESS)
            }
        } catch (e: Exception) {
            callback(Result.FAILURE)
        }
    }
}

@RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
suspend fun delete(
    context: Context
): Boolean {
    return GlobalScope.async(Dispatchers.IO) {
        val backupDir = context.getBackupPath()
        return@async backupDir.deleteRecursively()
    }.await()
}

@RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
suspend fun findBackup(
    context: Context,
    coroutineContext: CoroutineContext
): File? {
    return GlobalScope.async(coroutineContext) {
        val backupDir = context.getBackupPath()
        if (!backupDir.exists() || !backupDir.isDirectory) return@async null
        val files = backupDir.listFiles()
        if (files.isNullOrEmpty()) return@async null
        files.forEach { f ->
            val name = f.name
            val exists = try {
                val version = name.split('.')[2].toInt()
                version in Constants.DataBase.MINI_VERSION..Constants.DataBase.CURRENT_VERSION
            } catch (e: Exception) {
                false
            }
            if (exists) return@async f
        }
        null
    }.await()
}
