package one.mixin.android.util.backup

import com.drive.demo.backup.Result
import com.google.android.gms.drive.DriveFolder
import com.google.android.gms.drive.DriveResourceClient
import com.google.android.gms.drive.Metadata
import com.google.android.gms.drive.query.Filters
import com.google.android.gms.drive.query.Query
import com.google.android.gms.drive.query.SearchableField
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.extension.getMediaPath
import java.io.File
import java.util.concurrent.ExecutionException

class FileBackupManager private constructor(driveResourceClient: DriveResourceClient,
    private val folderName: String) : BackupManager(driveResourceClient) {

    companion object {
        private const val madieName = "Media"
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

    private fun createDirectory(): DriveFolder? {
        val metaData = isRootFolderExists(folderName)
        return if (metaData == null) {
            createRootFolder(folderName)
        } else {
            metaData.driveId.asDriveFolder()
        }
    }

    fun backup(callback: (Result) -> Unit) {
        GlobalScope.launch {
            val root = createDirectory()
            if (root == null) {
                callback(Result.FAILURE)
                return@launch
            }
            var mediaFolder = queryChildren(root, madieName)?.find {
                it.title == madieName && it.isFolder
            }?.driveId?.asDriveFolder()
            if (mediaFolder == null) {
                mediaFolder = createChildrenFolder(root, madieName)
            }
            mediaFolder?.let { mediaFolder ->
                contrastUpload(MixinApplication.appContext.getMediaPath(), mediaFolder)
                contrastDelete(MixinApplication.appContext.getMediaPath(), mediaFolder)
            }
        }
    }

    private fun contrastDelete(mediaPath: File, mediaFolder: DriveFolder) {
        folderForeach(mediaFolder)?.forEach {
            if (it.isFolder) {
                val file = File(mediaPath, it.title)
                if (file.exists() && file.isDirectory) {
                    contrastDelete(file, it.driveId.asDriveFolder())
                } else {
                    deleteDriveFile(it.driveId.asDriveResource())
                }
            } else {
                val file = File(mediaPath, it.title)
                if (!file.exists() || !file.isFile) {
                    deleteDriveFile(it.driveId.asDriveResource())
                }
            }
        }
    }

    private fun contrastUpload(mediaPath: File, mediaFolder: DriveFolder) {
        for (mediaChild in mediaPath.listFiles()) {
            if (mediaChild.isDirectory) {
                var mediaChildFolder = queryChildren(mediaFolder, mediaChild.name)?.find {
                    it.title == mediaChild.name && it.isFolder
                }?.driveId?.asDriveFolder()
                if (mediaChildFolder == null) {
                    mediaChildFolder = createChildrenFolder(mediaFolder, mediaChild.name)
                }
                for (file in mediaChild.listFiles()) {
                    if (isFileExists(mediaChildFolder!!, file.name) == null) {
                        upload(mediaChildFolder, file, file.name)
                    }
                }
            }
        }
    }

    fun findBackup(callback: (Result, Metadata?) -> Unit) {
        GlobalScope.launch {
            val parentMetadata = isRootFolderExists(folderName)
            if (parentMetadata == null) {
                withContext(Dispatchers.Main) {
                    callback(Result.NOT_FOUND, null)
                }
            }
            val metaData = findBackupFiles(parentMetadata!!.driveId.asDriveFolder(), madieName).run {
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
            val root = createDirectory()
            if (root == null) {
                callback(Result.FAILURE)
                return@launch
            }
            val mediaFolder = queryChildren(root, madieName)?.find {
                it.title == madieName && it.isFolder
            }?.driveId?.asDriveFolder()
            if (mediaFolder == null) {
                callback(Result.NOT_FOUND)
                return@launch
            }
            restoreToLocal(MixinApplication.appContext.getMediaPath(), mediaFolder, callback)
        }
    }

    private fun restoreToLocal(mediaPath: File, mediaFolder: DriveFolder, callback: (Result) -> Unit) {
        folderForeach(mediaFolder)?.forEach {
            if (it.isFolder) {
                val file = File("${mediaPath.absolutePath}${File.separator}${it.title}${File.separator}")
                if (!file.isDirectory) {
                    file.delete()
                }
                if (!file.exists()) {
                    file.mkdirs()
                }
                restoreToLocal(file, it.driveId.asDriveFolder(), callback)
            } else {
                val file = File(mediaPath, it.title)
                if (!file.exists() || !file.isFile) {
                    file.delete()
                    restore(it.driveId.asDriveFile(), file.absolutePath, callback)
                }
            }
        }
    }
}