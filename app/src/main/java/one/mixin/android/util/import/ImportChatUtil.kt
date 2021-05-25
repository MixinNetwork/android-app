package one.mixin.android.util.import

import android.content.Context
import android.net.Uri
import androidx.collection.ArrayMap
import androidx.collection.ArraySet
import androidx.collection.arrayMapOf
import one.mixin.android.MixinApplication
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.getFileName
import one.mixin.android.extension.getOtherPath
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.TranscriptMessage
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID
import java.util.regex.Pattern

class ImportChatUtil {

    companion object {
        private val exportUri = ArraySet<String>()

        init {
            exportUri.add("content://(\\d+@)?com\\.whatsapp\\.provider\\.media/export_chat/")
            exportUri.add("content://(\\d+@)?com\\.whatsapp\\.w4b\\.provider\\.media/export_chat/")
            exportUri.add("content://jp\\.naver\\.line\\.android\\.line\\.common\\.FileProvider/export-chat/")
            exportUri.add(".*WhatsApp.*\\.txt$")
        }

        @Synchronized
        fun get(): ImportChatUtil {
            if (instance == null) {
                instance = ImportChatUtil()
            }
            return instance as ImportChatUtil
        }

        private var instance: ImportChatUtil? = null
    }

    fun generateTranscriptMessage(context: Context, importUri: String, documents: List<String>): String? {
        var inputStream: InputStream? = null
        try {
            val documentsMap: ArrayMap<String, Uri> = arrayMapOf()
            documents.forEach {
                val uri = Uri.parse(it)
                val name = try {
                    fixFileName(uri.getFileName())
                } catch (e: Exception) {
                    null
                } ?: return@forEach
                documentsMap[name] = uri
            }
            inputStream = requireNotNull(context.contentResolver.openInputStream(Uri.parse(importUri)))
            val r = BufferedReader(InputStreamReader(inputStream))
            val list = arrayListOf<TranscriptMessage>()
            var line: String?
            while (r.readLine().also { line = it } != null) {
                line?.let {
                    generateTranscriptMessage(it, documentsMap)?.let { transcript ->
                        list.add(transcript)
                    }
                }
            }
            return GsonHelper.customGson.toJson(list)
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    // Todo
    private fun generateTranscriptMessage(s: String, documentsMap: ArrayMap<String, Uri>): TranscriptMessage? {
        val dateEnd = s.indexOf(" - ")
        val nameEnd = s.indexOf(": ")
        return if (dateEnd > 0 && nameEnd > 0) {
            val date = s.substring(0, dateEnd) // .toUtcTime()
            val name = s.substring(dateEnd + 3, nameEnd)
            val content = s.substring(nameEnd + 2, s.length)
            TranscriptMessage(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), name, MessageCategory.PLAIN_TEXT.name, date, content)
        } else {
            null
        }
    }

    fun getImportChatUri(uris: List<Uri>): Pair<Uri, List<Uri>>? {
        var exportingChatUri: Uri? = null
        val documentsUrisArray: ArrayList<Uri> = arrayListOf()
        uris.forEach forEach@{ uri ->
            val originalPath = uri.toString()
            val fileName = fixFileName(uri.getFileName())
            if (exportingChatUri == null) {
                for (u in exportUri) {
                    val pattern = Pattern.compile(u)
                    if (pattern.matcher(originalPath).find() || pattern.matcher(fileName).find()) {
                        exportingChatUri = uri
                        return@forEach
                    }
                }
            }
            documentsUrisArray.add(uri)
        }
        return exportingChatUri?.notNullWithElse({ Pair(it, documentsUrisArray) }, null)
    }

    fun copyFileToCache(uri: Uri, context: Context = MixinApplication.appContext) {
        val name = fixFileName(uri.getFileName())
        val backupFile = File("${context.getOtherPath().absolutePath}${File.separator}$name")
        Timber.d(backupFile.absolutePath)
        context.contentResolver.openInputStream(uri)?.let { backupFile.copyFromInputStream(it) }
    }

    private fun fixFileName(fileName: String): String {
        return fileName.replace("[\u0001-\u001f<>\u202E:\"/\\\\|?*\u007f]+", "").trim()
    }
}
