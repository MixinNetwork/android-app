package one.mixin.android.util.backup

import com.drive.demo.backup.Result
import com.google.android.gms.drive.DriveFile
import com.google.android.gms.drive.DriveFolder
import com.google.android.gms.drive.DriveId
import com.google.android.gms.drive.DriveResourceClient
import com.google.android.gms.drive.Metadata
import com.google.android.gms.drive.query.Filters
import com.google.android.gms.drive.query.Query
import com.google.android.gms.drive.query.SearchableField
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.extension.deleteDir
import one.mixin.android.extension.dirSize
import one.mixin.android.extension.getBackupPath
import one.mixin.android.extension.getMediaPath
import one.mixin.android.extension.moveChileFileToDir
import one.mixin.android.util.ZipUtil.unZipFolder
import one.mixin.android.util.ZipUtil.zipFolders
import java.io.File
import java.util.concurrent.ExecutionException
import kotlin.coroutines.CoroutineContext

class FileBackupManager private constructor(driveResourceClient: DriveResourceClient,
    private val folderName: String) : BackupManager(driveResourceClient) {

    companion object {
        private val backupName = "backup.zip"
        private val lock = Any()
        private var INSTANCE: FileBackupManager? = null

        fun getManager(driveResourceClient: DriveResourceClient, folderName: String): FileBackupManager {
            synchronized(lock) {
                if (INSTANCE == null || INSTANCE?.getFolderName() != folderName) {
                    INSTANCE = FileBackupManager(driveResourceClient, folderName)
                }
                return INSTANCE as FileBackupManager
            }
        }
    }

    fun getFolderName() = folderName

    private fun createDirectory() {
        val metaData = isFolderExists(folderName)
        if (metaData == null) {
            val parentFolderCreateTask = createFolder(folderName)
            Tasks.await(parentFolderCreateTask)
        }
    }

    fun backup(callback: (Result) -> Unit) {
        GlobalScope.launch {
            val zip = File("${MixinApplication.appContext.getBackupPath().apply {
                mkdirs()
            }.absolutePath}${File.separator}$backupName")
            val mediaPath = MixinApplication.appContext.getMediaPath()
            val mediaDirSize = mediaPath.dirSize()
            if (mediaDirSize != null && mediaDirSize > 0) {
                zipFolders(zip.absolutePath, mediaPath.absolutePath)
            } else {
                return@launch
            }
            createDirectory()
            val metadata = isFolderExists(folderName)
            if (metadata != null) {
                uploadDatabase(this.coroutineContext, metadata.driveId, zip, callback)
            } else {
                withContext(Dispatchers.Main) {
                    callback(Result.FAILURE)
                }
            }
        }
    }

    fun findBackup(callback: (Result, Metadata?) -> Unit) {
        GlobalScope.launch {
            val parentMetadata = isFolderExists(folderName)
            if (parentMetadata == null) {
                withContext(Dispatchers.Main) {
                    callback(Result.NOT_FOUND, null)
                }
            }
            val metaData = findBackupFiles(parentMetadata!!.driveId.asDriveFolder(), backupName).run {
                if (this.isNullOrEmpty()) {
                    null
                } else {
                    this[0]
                }
            }
            if (metaData != null) {
                withContext(Dispatchers.Main) {
                    callback(Result.SUCCESS, metaData)
                }
            } else {
                withContext(Dispatchers.Main) {
                    callback(Result.NOT_FOUND, null)
                }
            }
        }
    }

    private fun findBackupFiles(driveFile: DriveFolder, name: String): List<Metadata>? {
        val result = mutableListOf<Metadata>()
        val query = Query.Builder()
            .addFilter(Filters.eq(SearchableField.TITLE, name))
            .build()
        val queryTask = resourceClient.queryChildren(driveFile, query)
        try {
            val buffer = Tasks.await(queryTask)
            for (b in buffer) {
                val title = b.title
                if (title == name) {
                    result.add(b)
                }
            }
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return result
    }

    fun restore(callback: (Result) -> Unit) {
        GlobalScope.launch {
            val parentMetadata = isFolderExists(folderName)
            if (parentMetadata == null) {
                withContext(Dispatchers.Main) {
                    callback(Result.NOT_FOUND)
                }
            }
            val metaData = findBackupFiles(parentMetadata!!.driveId.asDriveFolder(), backupName)?.run {
                if (!this.isEmpty()) {
                    this[0]
                } else {
                    null
                }
            }

            if (metaData != null) {
                val zip = File("${MixinApplication.appContext.getBackupPath().apply {
                    mkdirs()
                }.absolutePath}${File.separator}$backupName")
                restore(metaData.driveId.asDriveFile(), zip.absolutePath, callback)
                if (zip.exists()) {
                    unZipFolder(zip.absolutePath, MixinApplication.appContext.getBackupPath().absolutePath)
                    for (item in zip.parentFile.listFiles()) {
                        if (item.isDirectory) {
                            for (i in item.listFiles()) {
                                if (i.isDirectory) {
                                    i.moveChileFileToDir(File("${MixinApplication.appContext.getMediaPath()}${File.separator}${i.name}${File.separator}"))
                                }
                            }
                            break
                        }
                    }
                    MixinApplication.appContext.getBackupPath().deleteDir()
                } else {
                    callback(Result.FAILURE)
                }
            } else {
                withContext(Dispatchers.Main) {
                    callback(Result.NOT_FOUND)
                }
            }
        }
    }

    private fun clearOtherBackup(driveFile: DriveFile) {
        val query = Query.Builder()
            .addFilter(Filters.eq(SearchableField.TITLE, backupName))
            .build()
        val queryTask = resourceClient.query(query)
        val metadataBuffer = Tasks.await(queryTask)
        for (b in metadataBuffer) {
            if (b.driveId != driveFile.driveId) {
                val deleteTask = deleteExistingFile(b.driveId.asDriveFile())
                Tasks.await(deleteTask)
            }
        }
    }

    private suspend fun uploadDatabase(context: CoroutineContext, driveId: DriveId, file: File, callback: (Result) -> Unit): DriveFile? {
        val d = GlobalScope.async(context) {
            val title = backupName
            val uploadTask = uploadBackup(driveId, file, title)
            val driveFile = Tasks.await(uploadTask)
            if (driveFile != null && uploadTask.isSuccessful) {
                withContext(Dispatchers.Main) {
                    callback(Result.SUCCESS)
                }
                clearOtherBackup(driveFile)
            } else {
                withContext(Dispatchers.Main) {
                    callback(Result.FAILURE)
                }
            }
            file.delete()
            return@async driveFile
        }
        return d.await()
    }
}