package one.mixin.android.util.mlkit

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractor
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.extension.isLowDisk
import one.mixin.android.util.reportException

private val mlExtractor by lazy {
    EntityExtraction.getClient(
        EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build(),
    )
}
private val conditions = DownloadConditions.Builder().build()

fun entityInitialize(): EntityExtractor {
    try {
        if (MixinApplication.get().isLowDisk().not()) {
            Tasks.await(mlExtractor.downloadModelIfNeeded(conditions))
        }
    } catch (e: Throwable) {
        reportException("MLKit init", e)
    }
    return mlExtractor
}

suspend fun firstUrl(input: String): String? = withContext(Dispatchers.IO) {
    return@withContext try {
        if (Tasks.await(mlExtractor.isModelDownloaded)) {
            val annotations = Tasks.await(mlExtractor.annotate(input))
            annotations.firstOrNull { annotation -> annotation.entities.any { entity -> entity.type == Entity.TYPE_URL } }?.annotatedText
        } else {
            if (MixinApplication.get().isLowDisk().not()) {
                Tasks.await(mlExtractor.downloadModelIfNeeded(conditions))
            }
            null
        }
    } catch (e: Throwable) {
        reportException("MLKit firstUrl", e)
        return@withContext null
    }
}
