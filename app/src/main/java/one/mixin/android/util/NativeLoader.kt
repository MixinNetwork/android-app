package one.mixin.android.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import one.mixin.android.extension.equalsIgnoreCase
import timber.log.Timber

private val libNames = listOf("mixin", "argon2jni", "barhopper_v3", "rlottie")
private val libSoNames = libNames.map { "lib$it.so" }
private val localLibSoNames = libNames.map { "lib${it}loc.so" }
private val nativeLoadedList = libNames.map { false }.toMutableList()

@SuppressLint("UnsafeDynamicallyLoadedCode")
@Suppress("DEPRECATION")
@Synchronized
fun initNativeLibs(context: Context) {
    if (nativeLoadedList.all { it }) return

    libNames.forEachIndexed { i, libName ->
        if (nativeLoadedList[i]) return@forEachIndexed

        try {
            try {
                System.loadLibrary(libName)
                nativeLoadedList[i] = true
                return@forEachIndexed
            } catch (e: Error) {
                Timber.e(e)
                reportException(e)
            }

            val destDir = File(context.filesDir, "lib")
            destDir.mkdirs()
            val destLocalFile = File(destDir, localLibSoNames[i])
            if (destLocalFile.exists()) {
                try {
                    Timber.d("Load local lib")
                    System.load(destLocalFile.absolutePath)
                    nativeLoadedList[i] = true
                    return@forEachIndexed
                } catch (e: Error) {
                    Timber.e(e)
                    reportException(e)
                }
                destLocalFile.delete()
            }

            var folder: String = try {
                when {
                    Build.CPU_ABI.equalsIgnoreCase("x86_64") -> {
                        "x86_64"
                    }
                    Build.CPU_ABI.equalsIgnoreCase("arm64-v8a") -> {
                        "arm64-v8a"
                    }
                    Build.CPU_ABI.equalsIgnoreCase("armeabi-v7a") -> {
                        "armeabi-v7a"
                    }
                    Build.CPU_ABI.equalsIgnoreCase("armeabi") -> {
                        "armeabi"
                    }
                    Build.CPU_ABI.equalsIgnoreCase("x86") -> {
                        "x86"
                    }
                    Build.CPU_ABI.equalsIgnoreCase("mips") -> {
                        "mips"
                    }
                    else -> {
                        Timber.e("Unsupported arch: ${Build.CPU_ABI}")
                        "armeabi"
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
                reportException(e)
                "armeabi"
            }
            val javaArch = System.getProperty("os.arch")
            if (javaArch != null && javaArch.contains("686")) {
                folder = "x86"
            }
            Timber.e("Lib bot found, arch = $folder")

            if (loadFromZip(context, destDir, destLocalFile, folder, i)) {
                return@forEachIndexed
            }
        } catch (e: Throwable) {
            Timber.e(e)
            reportException(e)
        }

        try {
            System.loadLibrary(libName)
            nativeLoadedList[i] = true
        } catch (e: Error) {
            Timber.e(e)
            reportException(e)
        }
    }
}

@SuppressLint("SetWorldReadable", "UnsafeDynamicallyLoadedCode")
private fun loadFromZip(context: Context, destDir: File, destLocalFile: File, folder: String, i: Int): Boolean {
    try {
        destDir.listFiles()?.let { files ->
            for (f in files) {
                f.delete()
            }
        }
    } catch (e: Exception) {
        Timber.e(e)
        reportException(e)
    }

    ZipFile(context.applicationInfo.sourceDir).use { zipFile ->
        val entry: ZipEntry = zipFile.getEntry("lib/$folder/${libSoNames[i]}")
            ?: throw Exception("Unable to find file in apk:lib/$folder/${libNames[i]}")
        zipFile.getInputStream(entry).use { input ->
            FileOutputStream(destLocalFile).use { output ->
                input.copyTo(output)
            }
        }
    }
    destLocalFile.setReadable(true, false)
    destLocalFile.setExecutable(true, false)
    destLocalFile.setWritable(true)

    try {
        System.load(destLocalFile.absolutePath)
        nativeLoadedList[i] = true
        return true
    } catch (e: Error) {
        Timber.e(e)
        reportException(e)
    }
    return false
}
