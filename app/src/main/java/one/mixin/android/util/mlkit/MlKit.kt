package one.mixin.android.util.mlkit

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.extension.isLowDisk
import one.mixin.android.util.reportException

private val mlExtractor by lazy {
    return@lazy try {
        EntityExtraction.getClient(
            EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build(),
        )
    } catch (e: Exception) {
        reportException(e)
        null
    }
}
private val conditions = DownloadConditions.Builder().build()

suspend fun entityInitialize() {
    withContext(Dispatchers.IO) {
        val extractor = mlExtractor ?: return@withContext
        try {
            if (MixinApplication.get().isLowDisk().not()) {
                Tasks.await(extractor.downloadModelIfNeeded(conditions))
            }
        } catch (e: Exception) {
            reportException(e)
        }
    }
}

suspend fun firstUrl(input: String): String? = withContext(Dispatchers.IO) {
    val extractor = mlExtractor ?: return@withContext null
    return@withContext try {
        if (Tasks.await(extractor.isModelDownloaded)) {
            val annotations = Tasks.await(extractor.annotate(input))
            annotations.firstOrNull { annotation -> annotation.entities.any { entity -> entity.type == Entity.TYPE_URL } }?.annotatedText
        } else {
            entityInitialize()
            null
        }
    } catch (e: Throwable) {
        reportException("MLKit firstUrl", e)
        return@withContext null
    }
}
