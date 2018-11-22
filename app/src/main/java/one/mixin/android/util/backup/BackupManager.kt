package one.mixin.android.util.backup

import com.drive.demo.backup.Result
import com.drive.demo.backup.Result.FAILURE
import com.drive.demo.backup.Result.SUCCESS
import com.google.android.gms.drive.DriveFile
import com.google.android.gms.drive.DriveFolder
import com.google.android.gms.drive.DriveId
import com.google.android.gms.drive.DriveResourceClient
import com.google.android.gms.drive.Metadata
import com.google.android.gms.drive.MetadataChangeSet
import com.google.android.gms.drive.query.Filters
import com.google.android.gms.drive.query.Query
import com.google.android.gms.drive.query.SearchableField
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ExecutionException
import kotlin.math.min

open class BackupManager(val resourceClient: DriveResourceClient) {

    fun uploadBackup(driveId: DriveId, file: File, title: String, callback: ((Int) -> Unit)? = null): Task<DriveFile> {
        val rootFolder = resourceClient.appFolder
        val createContents = resourceClient.createContents()
        return Tasks.whenAll(rootFolder, createContents)
            .continueWithTask<DriveFile> {
                val parent = driveId.asDriveFolder()
                val contents = createContents.result!!
                val outputStream = contents.outputStream
                val fis = FileInputStream(file)
                val buffer = ByteArray(1024)
                val total = file.length()
                var currentSize = 0L
                var length: Int
                do {
                    length = fis.read(buffer)
                    if (length > 0) {
                        outputStream.write(buffer, 0, length)
                        currentSize += length
                        callback?.invoke(min((100f * currentSize / total).toInt(), 100))
                    } else break
                } while (true)
                outputStream.flush()
                outputStream.close()
                fis.close()
                val changeSet = MetadataChangeSet.Builder()
                    .setTitle(title)
                    .setStarred(false)
                    .build()
                resourceClient.createFile(parent, changeSet, contents)
            }
    }

    fun restore(driveFile: DriveFile, file: String, callback: (Result) -> Unit) {
        val openFileTask = resourceClient.openFile(driveFile, DriveFile.MODE_READ_ONLY);
        val discard = openFileTask.continueWith { task ->
            val contents = task.result
            val parcelFileDescriptor = contents!!.parcelFileDescriptor
            val fis = FileInputStream(parcelFileDescriptor.fileDescriptor)
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(1024)
            var length: Int
            do {
                length = fis.read(buffer)
                if (length > 0) {
                    outputStream.write(buffer, 0, length)
                } else break
            } while (true)
            outputStream.flush()
            outputStream.close()
            fis.close()
            resourceClient.discardContents(contents)
        }.addOnSuccessListener {
            GlobalScope.launch(Dispatchers.Main) {
                callback(SUCCESS)
            }
        }.addOnFailureListener {
            GlobalScope.launch(Dispatchers.Main) {
                callback(FAILURE)
            }
        }
        Tasks.await(discard)
    }

    fun deleteExistingFile(fileName: String) {
        val query = Query.Builder()
            .addFilter(Filters.eq(SearchableField.TITLE, fileName))
            .build()
        val queryTask = resourceClient.query(query)
        val metadataBuffer = Tasks.await(queryTask)
        for (b in metadataBuffer) {
            if (b.title == fileName) {
                val deleteTask = deleteExistingFile(b.driveId.asDriveFile())
                Tasks.await(deleteTask)
            }
        }
    }

    fun deleteExistingFile(file: DriveFile): Task<Void> {
        return resourceClient.delete(file)
    }

    protected fun isFolderExists(folderName: String): Metadata? {
        var metadata: Metadata? = null
        val query = Query.Builder()
            .addFilter(Filters.eq(SearchableField.TITLE, folderName))
            .build()
        val queryTask = resourceClient.query(query)
        try {
            val buffer = Tasks.await(queryTask)
            for (b in buffer) {
                if (b.title == folderName) {
                    metadata = b
                    break
                }
            }
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return metadata
    }

    fun createFolder(folderName: String): Task<DriveFolder> {
        return resourceClient
            .appFolder
            .continueWithTask { task ->
                val parentFolder = task.result!!
                val changeSet = MetadataChangeSet.Builder()
                    .setTitle(folderName)
                    .setMimeType(DriveFolder.MIME_TYPE)
                    .setStarred(false)
                    .build()
                resourceClient.createFolder(parentFolder, changeSet)
            }
    }
}
