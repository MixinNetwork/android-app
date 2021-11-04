package one.mixin.android.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import one.mixin.android.extension.equalsIgnoreCase
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

private const val libName = "mixin"
private const val libSoName = "lib$libName.so"
private const val localLibSoName = "lib${libName}loc.so"

private var nativeLoaded = false

@SuppressLint("UnsafeDynamicallyLoadedCode")
@Suppress("DEPRECATION")
@Synchronized
fun initNativeLibs(context: Context) {
    if (nativeLoaded) return

    try {
        try {
            System.loadLibrary(libName)
            nativeLoaded = true
            return
        } catch (e: Error) {
            Timber.e(e)
            reportException(e)
        }

        val destDir = File(context.filesDir, "lib")
        destDir.mkdirs()
        val destLocalFile = File(destDir, localLibSoName)
        if (destLocalFile.exists()) {
            try {
                Timber.d("Load local lib")
                System.load(destLocalFile.absolutePath)
                nativeLoaded = true
                return
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

        if (loadFromZip(context, destDir, destLocalFile, folder)) {
            return
        }
    } catch (e: Throwable) {
        Timber.e(e)
        reportException(e)
    }

    try {
        System.loadLibrary(libName)
        nativeLoaded = true
    } catch (e: Error) {
        Timber.e(e)
        reportException(e)
    }
}

@SuppressLint("SetWorldReadable", "UnsafeDynamicallyLoadedCode")
private fun loadFromZip(context: Context, destDir: File, destLocalFile: File, folder: String): Boolean {
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
        val entry: ZipEntry = zipFile.getEntry("lib/$folder/$libSoName")
            ?: throw Exception("Unable to find file in apk:lib/$folder/$libName")
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
        nativeLoaded = true
        return true
    } catch (e: Error) {
        Timber.e(e)
        reportException(e)
    }
    return false
}
