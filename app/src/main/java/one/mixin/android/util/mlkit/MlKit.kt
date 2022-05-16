package one.mixin.android.util.mlkit

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val mlExtractor by lazy {
    EntityExtraction.getClient(
        EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
    )
}

suspend fun entityInitialize() {
    withContext(Dispatchers.IO) {
        Tasks.await(mlExtractor.downloadModelIfNeeded())
    }
}

suspend fun firstUrl(input: String): String? = withContext(Dispatchers.IO) {
    return@withContext if (Tasks.await(mlExtractor.isModelDownloaded)) {
        val annotations = Tasks.await(mlExtractor.annotate(input))
        annotations.first { annotation -> annotation.entities.any { entity -> entity.type == Entity.TYPE_URL } }.annotatedText
    } else {
        Tasks.await(mlExtractor.downloadModelIfNeeded())
        null
    }
}
