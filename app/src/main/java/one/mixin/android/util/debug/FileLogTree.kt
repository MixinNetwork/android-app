package one.mixin.android.util.debug

import android.util.Log
import one.mixin.android.BuildConfig
import one.mixin.android.MixinApplication
import one.mixin.android.extension.copy
import one.mixin.android.extension.nowInUtc
import one.mixin.android.util.ZipUtil
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FileLogTree : Timber.Tree() {
    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        if (priority == Log.ERROR || priority == Log.ASSERT) {
            try {
                val directory = MixinApplication.appContext.cacheDir

                val file =
                    if (priority == Log.ERROR) {
                        File("${directory.absolutePath}${File.separator}$LOG_LOCAL_FILE_NAME")
                    } else {
                        File("${directory.absolutePath}${File.separator}$LOG_FILE_NAME")
                    }
                file.createNewFile()
                if (file.exists()) {
                    if (file.length() >= MAX_SIZE) {
                        file.delete()
                        file.createNewFile()
                    }
                    if (file.length() == 0L) {
                        file.outputStream().use {
                            it.write("Mixin${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})\n".toByteArray(Charsets.UTF_8))
                        }
                    }
                    val fos = FileOutputStream(file, true)
                    fos.write("${nowInUtc()} $message\n".toByteArray(Charsets.UTF_8))
                    fos.close()
                }
            } catch (e: IOException) {
                Log.println(Log.ERROR, "FileLogTree", "Error while logging into file: $e")
            }
        }
    }

    companion object {
        private const val LOG_LOCAL_FILE_NAME = "mixin"
        private const val LOG_FILE_NAME = "mixin.log"
        private const val LOG_ZIP_FILE_NAME = "mixin.zip"
        private const val LOG_ZIP_FOLDER_NAME = "zip"
        private const val MAX_SIZE = 512 * 1024 * 1024

        fun getLogFile(): File {
            val directory = MixinApplication.appContext.cacheDir
            val zipFile = File("${directory.absolutePath}${File.separator}$LOG_ZIP_FILE_NAME")
            val zipFolder = File("${directory.absolutePath}${File.separator}$LOG_ZIP_FOLDER_NAME")
            if (zipFolder.exists()) {
                zipFolder.delete()
            }
            zipFolder.mkdirs()
            val file = File("${directory.absolutePath}${File.separator}$LOG_LOCAL_FILE_NAME")
            if (file.exists()) {
                file.copy(File(zipFolder, LOG_LOCAL_FILE_NAME))
            }
            val lopFile = File("${directory.absolutePath}${File.separator}$LOG_FILE_NAME")
            if (lopFile.exists()) {
                lopFile.copy(File(zipFolder, LOG_FILE_NAME))
            }
            ZipUtil.zipFolder(zipFolder.absolutePath, zipFile.absolutePath)
            zipFolder.deleteRecursively()
            return zipFile
        }
    }
}
