package one.mixin.android.util.backup

import one.mixin.android.util.backup.Result.FAILURE
import one.mixin.android.util.backup.Result.SUCCESS
//import com.google.android.gms.drive.DriveFile
//import com.google.android.gms.drive.DriveId
//import com.google.android.gms.drive.DriveResource
//import com.google.android.gms.drive.DriveResourceClient
//import com.google.android.gms.drive.Metadata
//import com.google.android.gms.drive.MetadataBuffer
//import com.google.android.gms.drive.MetadataChangeSet
//import com.google.android.gms.drive.query.Filters
//import com.google.android.gms.drive.query.Query
//import com.google.android.gms.drive.query.SearchableField
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import one.mixin.android.util.backup.drive.DriveFolder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ExecutionException
import kotlin.coroutines.CoroutineContext

open class BackupManager(protected val driveManager: DriveManager) {

    protected suspend fun upload(
        context: CoroutineContext,
        folder: com.google.api.services.drive.model.File,
        file: File,
        name: String
    ): com.google.api.services.drive.model.File {
        return GlobalScope.async(context) {
            driveManager.upload(file, "application/zip", name)
        }.await()
    }

//    protected fun upload(driveFile: DriveFolder, file: File, title: String): Task<DriveFile>? {
//        val createContents = resourceClient.createContents()
//        return Tasks.await(createContents.continueWith { task ->
//            val contents = task.result!!
//            val outputStream = contents.outputStream
//            val fis = FileInputStream(file)
//            val buffer = ByteArray(1024)
//            var currentSize = 0L
//            var length: Int
//            do {
//                length = fis.read(buffer)
//                if (length > 0) {
//                    outputStream.write(buffer, 0, length)
//                    currentSize += length
//                } else break
//            } while (true)
//            outputStream.flush()
//            outputStream.close()
//            fis.close()
//            val changeSet = MetadataChangeSet.Builder()
//                .setTitle(title)
//                .setStarred(false)
//                .build()
//            resourceClient.createFile(driveFile, changeSet, contents)
//        })
//    }

    suspend fun restore(
        context: CoroutineContext,
        driveFile: com.google.api.services.drive.model.File,
        filePath: String,
        callback: (Result) -> Unit
    ) {
        GlobalScope.async(context) {
            try {
                driveManager.download(driveFile.id, filePath)
                GlobalScope.launch(Dispatchers.Main) {
                    callback(SUCCESS)
                }
            } catch (e: ExecutionException) {
                GlobalScope.launch(Dispatchers.Main) {
                    callback(FAILURE)
                }
            }
        }.await()
    }

//    fun restore(driveFile: DriveFile, file: String, callback: (Result) -> Unit) {
//        val openFileTask = resourceClient.openFile(driveFile, DriveFile.MODE_READ_ONLY)
//        val discard = openFileTask.continueWith { task ->
//            val contents = task.result
//            val parcelFileDescriptor = contents!!.parcelFileDescriptor
//            val fis = FileInputStream(parcelFileDescriptor.fileDescriptor)
//            val outputStream = FileOutputStream(file)
//            val buffer = ByteArray(1024)
//            var length: Int
//            do {
//                length = fis.read(buffer)
//                if (length > 0) {
//                    outputStream.write(buffer, 0, length)
//                } else break
//            } while (true)
//            outputStream.flush()
//            outputStream.close()
//            fis.close()
//            resourceClient.discardContents(contents)
//        }.addOnSuccessListener {
//            GlobalScope.launch(Dispatchers.Main) {
//                callback(SUCCESS)
//            }
//        }.addOnFailureListener {
//            GlobalScope.launch(Dispatchers.Main) {
//                callback(FAILURE)
//            }
//        }
//        Tasks.await(discard)
//    }

//    fun deleteExistingFile(fileName: String) {
//        val query = Query.Builder()
//            .addFilter(Filters.eq(SearchableField.TITLE, fileName))
//            .build()
//        val queryTask = resourceClient.query(query)
//        val metadataBuffer = Tasks.await(queryTask)
//        for (b in metadataBuffer) {
//            if (b.title == fileName) {
//                val deleteTask = deleteDriveFile(b.driveId.asDriveFile())
//                Tasks.await(deleteTask)
//            }
//        }
//    }
//
//    fun deleteDriveFile(file: DriveResource): Task<Void> {
//        return resourceClient.delete(file)
//    }

    protected fun isRootFolderExists(folderName: String): com.google.api.services.drive.model.File? {
        return driveManager.isAppFolderExists()
    }

    protected fun isRootFolderExists(folderName: String): Metadata? {
        val appFolder = Tasks.await(resourceClient.appFolder) ?: return null
        val buffer = queryChildren(appFolder, folderName) ?: return null
        for (item in buffer) {
            if (item.title == folderName) {
                return item
            }
        }
        return null
    }

    protected fun isFileExists(driveFolder: DriveFolder, fileName: String): Metadata? {
        val buffer = queryChildren(driveFolder, fileName) ?: return null
        return buffer.find { item ->
            !item.isFolder
        }
    }

    protected fun queryChildren(driveFolder: DriveFolder, name: String): MetadataBuffer? {
        val query = Query.Builder()
            .addFilter(Filters.eq(SearchableField.TITLE, name))
            .build()
        return Tasks.await(resourceClient.queryChildren(driveFolder, query))
    }

    protected fun folderForeach(driveFolder: DriveFolder): MetadataBuffer? {
        return Tasks.await(resourceClient.listChildren(driveFolder))
    }

    protected fun createRootFolder(folderName: String): DriveFolder? {
        val appFolder = Tasks.await(resourceClient.appFolder) ?: return null
        return createChildrenFolder(appFolder, folderName)
    }

    protected fun createChildrenFolder(driveFolder: DriveFolder, folderName: String): DriveFolder? {
        val changeSet = MetadataChangeSet.Builder()
            .setTitle(folderName)
            .setMimeType(DriveFolder.MIME_TYPE)
            .setStarred(false)
            .build()
        return Tasks.await(resourceClient.createFolder(driveFolder, changeSet))
    }
}
