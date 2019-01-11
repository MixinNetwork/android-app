package one.mixin.android.util.backup

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.FileContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import one.mixin.android.util.SingletonHolder
import one.mixin.android.util.backup.drive.Query
import java.io.FileOutputStream
import java.util.Collections

class DriveManager private constructor(context: Context) {

    companion object : SingletonHolder<DriveManager, Context>(::DriveManager) {
        const val FOLDER_MIME_TYPE = "application/vnd.google-apps.drive-sdk"
    }

    private val drive: Drive = Drive.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        JacksonFactory.getDefaultInstance(),
        GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA))
    ).build()

    fun upload(file: java.io.File,
        mimeType: String,
        name: String,
        block: (Drive.Files.Create.() -> Drive.Files.Create)? = null
    ): File {
        val fileMeta = File()
        fileMeta.name = name
        fileMeta.parents = Collections.singletonList("appDataFolder")
        val fileContent = FileContent(mimeType, file)
        var create = drive.Files()
            .create(fileMeta, fileContent)
        create = if (block != null) {
            create.block()
        } else {
            create
        }
        return create.execute()
    }

    fun download(fileId: String, filePath: String) {
        val outputStream = FileOutputStream(filePath)
        drive.files()
            .get(fileId)
            .executeAndDownloadTo(outputStream)
    }

    fun isAppFolderExists(): Boolean {
        val appDataFolder = drive.files().list()
            .setSpaces("appDataFolder")
            .execute()
        return appDataFolder != null && appDataFolder.isNotEmpty()
    }

    fun query(name: String): List<File>? {
        val queryString = "name = '$name'"
        val queryResult = drive.files()
            .list()
            .setQ(queryString)
            .execute()
        return queryResult.files
    }

    fun delete(file: File) {
        drive.files().delete(file.id)
    }

    fun queryChildren(
        folder: File,
        fileName: String
    ): List<File>? {
        val folderQuery = Query.build {
            filters = mutableListOf<String>().apply {
                add("mimeType = 'application/vnd.google-apps.drive-sdk'")
                add("name = '${folder.name}'")
            }
        }
        val folders = drive.files()
            .list()
            .setQ(folderQuery.toString())
            .execute()
        val result = mutableListOf<File>()
        folders.files.forEach {
            val fileQuery = Query.build {
                filters = mutableListOf<String>().apply {
                    add("mimeType != 'application/vnd.google-apps.folder'")
                    add("'${it.id}' in parents")
                    add("name = '$fileName'")
                }
            }
            val queryResult = drive.files()
                .list()
                .setQ(fileQuery.toString())
                .execute()
            result.addAll(queryResult.files)
        }
        return result
    }
}