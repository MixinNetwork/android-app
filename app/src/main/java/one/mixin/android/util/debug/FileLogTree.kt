package one.mixin.android.util.debug

import android.util.Log
import one.mixin.android.MixinApplication
import one.mixin.android.extension.nowInUtc
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FileLogTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority >= Log.ERROR) {
            try {
                val directory = MixinApplication.appContext.cacheDir

                if (!directory.exists()) {
                    directory.mkdirs()
                }

                val file = File("${directory.absolutePath}${File.separator}$LOG_LOCAL_FILE_NAME")
                file.createNewFile()
                if (file.exists()) {
                    if (file.length() >= MAX_SIZE) {
                        file.delete()
                        file.createNewFile()
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
        private const val MAX_SIZE = 5 * 1024 * 1024
        fun getLogFile(): File? {
            val directory = MixinApplication.appContext.cacheDir
            val file = File("${directory.absolutePath}${File.separator}$LOG_LOCAL_FILE_NAME")
            return if (file.exists()) {
                val result = File("${directory.absolutePath}${File.separator}$LOG_FILE_NAME")
                file.copyTo(result, true)
                result
            } else {
                null
            }
        }
    }
}
