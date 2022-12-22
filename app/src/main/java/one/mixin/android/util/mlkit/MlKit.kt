package one.mixin.android.util.mlkit

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.util.reportException

private val mlExtractor by lazy {
    EntityExtraction.getClient(
        EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build(),
    )
}
private val conditions = DownloadConditions.Builder().build()

suspend fun entityInitialize() {
    withContext(Dispatchers.IO) {
        try {
            Tasks.await(mlExtractor.downloadModelIfNeeded(conditions))
        } catch (e: Throwable) {
            reportException("MLKit init", e)
        }
    }
}

suspend fun firstUrl(input: String): String? = withContext(Dispatchers.IO) {
    return@withContext try {
        if (Tasks.await(mlExtractor.isModelDownloaded)) {
            val annotations = Tasks.await(mlExtractor.annotate(input))
            annotations.firstOrNull { annotation -> annotation.entities.any { entity -> entity.type == Entity.TYPE_URL } }?.annotatedText
        } else {
            Tasks.await(mlExtractor.downloadModelIfNeeded(conditions))
            null
        }
    } catch (e: Throwable) {
        reportException("MLKit firstUrl", e)
        return@withContext null
    }
}
