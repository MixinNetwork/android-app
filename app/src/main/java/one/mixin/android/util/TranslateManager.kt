package one.mixin.android.util

import android.content.Context
import android.util.LruCache
import androidx.recyclerview.widget.DiffUtil
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import okhttp3.internal.closeQuietly
import one.mixin.android.Constants.Account.PREF_TRANSLATE_TARGET_LANG
import one.mixin.android.extension.defaultSharedPreferences
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TranslateManager {
    private val executor = Executors.newCachedThreadPool()
    private val languageIdentifier = LanguageIdentification.getClient(
        LanguageIdentificationOptions.Builder()
            .setExecutor(executor)
            .setConfidenceThreshold(0.5f)
            .build(),
    )
    private val translators =
        object : LruCache<TranslatorOptions, Translator>(3) {
            override fun create(options: TranslatorOptions): Translator {
                return Translation.getClient(options)
            }

            override fun entryRemoved(
                evicted: Boolean,
                key: TranslatorOptions,
                oldValue: Translator,
                newValue: Translator?,
            ) {
                oldValue.close()
            }
        }
    private val modelManager = RemoteModelManager.getInstance()

    fun translate(
        context: Context,
        text: String,
        targetLang: String? = context.defaultSharedPreferences.getString(PREF_TRANSLATE_TARGET_LANG, getLanguageOrDefault()),
        doWhenDownloadModel: ((Boolean) -> Unit)? = null, // false/true represents download start/end
    ): String? {
        if (targetLang.isNullOrBlank()) {
            Timber.d("$TAG targetLang is $targetLang")
            return null
        }
        var latch = CountDownLatch(1)
        val identifierTask = languageIdentifier.identifyLanguage(text).addOnCompleteListener {
            latch.countDown()
        }
        val identifierDone = latch.await(TASK_TIMEOUT, TimeUnit.SECONDS)
        if (!identifierDone) {
            Timber.d("$TAG languageIdentifier timeout ${identifierTask.exception?.stackTraceToString()}")
            return null
        }
        val sourceLangCode = TranslateLanguage.fromLanguageTag(identifierTask.result)
        val targetLangCode = TranslateLanguage.fromLanguageTag(targetLang)
        if (sourceLangCode == null || targetLangCode == null) {
            Timber.d("$TAG sourceLangCode: $sourceLangCode, targetLangCode: $targetLangCode")
            return null
        }

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLangCode)
            .setTargetLanguage(targetLangCode)
            .setExecutor(executor)
            .build()
        val translator = translators[options]

        val needDownload: Boolean
        var checkDownloadTask = isModelDownload(sourceLangCode)
        latch = CountDownLatch(1)
        var checkDownloadResult = checkDownloadTask.addOnCompleteListener {
            latch.countDown()
        }
        var checkResult = latch.await(TASK_TIMEOUT, TimeUnit.SECONDS)
        if (!checkResult || !checkDownloadResult.isSuccessful) {
            Timber.d("$TAG check source download timeout or failed")
            return null
        }
        // if source model downloaded continue check target model, otherwise go download.
        if (checkDownloadResult.result) {
            checkDownloadTask = isModelDownload(targetLangCode)
            latch = CountDownLatch(1)
            checkDownloadResult = checkDownloadTask.addOnCompleteListener {
                latch.countDown()
            }
            checkResult = latch.await(TASK_TIMEOUT, TimeUnit.SECONDS)
            if (!checkResult || !checkDownloadResult.isSuccessful) {
                Timber.d("$TAG check target download timeout or failed")
                return null
            }
            needDownload = !checkDownloadResult.result
        } else {
            needDownload = true
        }

        if (needDownload) {
            doWhenDownloadModel?.invoke(false)
            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()
            latch = CountDownLatch(1)
            val downloadTask = translator.downloadModelIfNeeded(conditions).addOnCompleteListener {
                latch.countDown()
            }
            val downloaded = latch.await(TASK_TIMEOUT, TimeUnit.SECONDS)
            doWhenDownloadModel?.invoke(true)
            if (!downloaded || !downloadTask.isSuccessful) {
                Timber.d("$TAG download model timeout or failed")
                return null
            }
        }

        latch = CountDownLatch(1)
        val task = translator.translate(text).addOnCompleteListener {
            latch.countDown()
        }
        val translated = latch.await(TASK_TIMEOUT, TimeUnit.SECONDS)
        if (!translated || !task.isSuccessful) {
            Timber.d("$TAG translate timeout or failed")
            return null
        }
        return task.result
    }

    private fun isModelDownload(code: String): Task<Boolean> {
        val translateLanguage = TranslateLanguage.fromLanguageTag(code) ?: return Tasks.forResult(false)
        return modelManager.isModelDownloaded(TranslateRemoteModel.Builder(translateLanguage).build())
    }

    fun release() {
        languageIdentifier.closeQuietly()
        translators.evictAll()
    }

    class Language(val code: String) : Comparable<Language> {
        private val locale = Locale(code)
        val nameInCurrentLanguage: String = locale.getDisplayName(getLocale())
        val nameInSelfLanguage: String = locale.getDisplayLanguage(locale)

        override fun equals(other: Any?): Boolean {
            if (other === this) {
                return true
            }

            if (other !is Language) {
                return false
            }

            val otherLang = other as Language?
            return otherLang!!.code == code
        }

        override fun toString(): String {
            return nameInCurrentLanguage
        }

        override fun compareTo(other: Language): Int {
            return this.nameInCurrentLanguage.compareTo(other.nameInCurrentLanguage)
        }

        override fun hashCode(): Int {
            return code.hashCode()
        }

        companion object {
            val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Language>() {
                override fun areItemsTheSame(oldItem: Language, newItem: Language): Boolean = oldItem.code == newItem.code

                override fun areContentsTheSame(oldItem: Language, newItem: Language): Boolean = oldItem == newItem
            }
        }
    }
    companion object {
        const val TAG = "TranslateManager"

        private const val TASK_TIMEOUT = 5L
    }
}
