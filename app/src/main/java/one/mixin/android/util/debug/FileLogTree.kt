package one.mixin.android.util.debug
import android.util.Log
import one.mixin.android.MixinApplication
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FileLogTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {

        if (priority >= Log.ERROR) {
            try {
                val directory = MixinApplication.appContext.cacheDir

                if (!directory.exists())
                    directory.mkdirs()

                val file = File("${directory.absolutePath}${File.separator}$LOG_FILE_NAME")
                file.createNewFile()
                if (file.exists()) {
                    val fos = FileOutputStream(file, true)
                    fos.write("$message\n".toByteArray(Charsets.UTF_8))
                    fos.close()
                }
            } catch (e: IOException) {
                Log.println(Log.ERROR, "FileLogTree", "Error while logging into file: $e")
            }
        }
    }

    companion object {
        private const val LOG_FILE_NAME = "mixin.log"
        fun getLogFile(): File? {
            val directory = MixinApplication.appContext.cacheDir
            val file = File("${directory.absolutePath}${File.separator}$LOG_FILE_NAME")
            return if (file.exists()) {
                file
            } else {
                null
            }
        }
    }
}
