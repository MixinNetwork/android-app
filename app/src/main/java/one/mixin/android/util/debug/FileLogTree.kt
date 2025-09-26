package one.mixin.android.util.debug

import android.os.Build
import android.provider.Settings
import android.util.Log
import one.mixin.android.BuildConfig
import one.mixin.android.Constants.Account.PREF_LOGIN_VERIFY
import one.mixin.android.MixinApplication
import one.mixin.android.extension.copy
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.nowInUtc
import one.mixin.android.session.Session
import one.mixin.android.ui.landing.LandingActivity
import one.mixin.android.ui.tip.TipActivity
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
        val directory = MixinApplication.appContext.cacheDir
        val defaultSharedPreferences = MixinApplication.appContext.defaultSharedPreferences
        // not logged in, safe not set, PIN verification not completed - record log
        if (Session.getAccountId() == null || !Session.hasSafe() || MixinApplication.get().topActivity is LandingActivity || MixinApplication.get().topActivity is TipActivity || defaultSharedPreferences.getBoolean(PREF_LOGIN_VERIFY, false)) {
            if (priority >= Log.INFO) {
                try {
                    val file = File("${directory.absolutePath}${File.separator}$LOG_PRE_LOGIN_FILE_NAME")
                    file.createNewFile()
                    if (file.exists()) {
                        if (file.length() >= MAX_LOGIN_SIZE) {
                            file.delete()
                            file.createNewFile()
                        }
                        if (file.length() == 0L) {
                            val account = Session.getAccount()
                            file.outputStream().use {
                                it.write("Brand: ${Build.BRAND}\n".toByteArray(Charsets.UTF_8))
                                it.write("App Version: ${BuildConfig.VERSION_CODE}\n".toByteArray(Charsets.UTF_8))
                                it.write("App Version Code: ${BuildConfig.VERSION_NAME}\n".toByteArray(Charsets.UTF_8))
                                it.write("App ID: ${BuildConfig.APPLICATION_ID}\n".toByteArray(Charsets.UTF_8))
                                it.write("Device ID: ${Settings.Secure.getString(MixinApplication.appContext.contentResolver, Settings.Secure.ANDROID_ID)}\n".toByteArray(Charsets.UTF_8))
                                it.write("Model: ${Build.MODEL}\n".toByteArray(Charsets.UTF_8))
                                it.write("ID: ${Build.ID}\n".toByteArray(Charsets.UTF_8))
                                it.write("SDK: ${Build.VERSION.SDK_INT}\n".toByteArray(Charsets.UTF_8))
                                it.write("Incremental: ${Build.VERSION.INCREMENTAL}\n".toByteArray(Charsets.UTF_8))
                                it.write("Version Code: ${Build.VERSION.RELEASE}\n".toByteArray(Charsets.UTF_8))

                                if (account != null) {
                                    it.write("Account userId: ${account.userId}\n".toByteArray(Charsets.UTF_8))
                                    it.write("Account hasSafe: ${account.hasSafe}\n".toByteArray(Charsets.UTF_8))
                                    it.write("Account tipCounter: ${account.tipCounter}\n".toByteArray(Charsets.UTF_8))
                                }
                            }
                        }
                        val fos = FileOutputStream(file, true)
                        fos.write("${nowInUtc()} $message\n".toByteArray(Charsets.UTF_8))
                        fos.close()
                    }
                } catch (e: IOException) {
                    Log.println(Log.ERROR, "FileLogTree", "Error while logging into pre-login file: $e")
                }
            }
        }

        if (priority == Log.ERROR || priority == Log.ASSERT) {
            try {
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
        private const val LOG_PRE_LOGIN_FILE_NAME = "mixin_pre_login.log"
        private const val LOG_LOCAL_FILE_NAME = "mixin.txt"
        private const val LOG_FILE_NAME = "mixin.log"
        private const val LOG_ZIP_FILE_NAME = "mixin.zip"
        private const val LOG_ZIP_FOLDER_NAME = "zip"
        private const val MAX_SIZE = 512 * 1024 * 1024
        private const val MAX_LOGIN_SIZE = 256 * 1024

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
            file.deleteOnExit()
            zipFolder.deleteRecursively()
            return zipFile
        }

        fun getPreLoginLogFile(): File? {
            val directory = MixinApplication.appContext.cacheDir
            val file = File("${directory.absolutePath}${File.separator}$LOG_PRE_LOGIN_FILE_NAME")
            return if (file.exists()) file else null
        }
    }
}
