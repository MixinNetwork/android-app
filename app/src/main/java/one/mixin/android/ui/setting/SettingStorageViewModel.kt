package one.mixin.android.ui.setting

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.Storage.AUDIO
import one.mixin.android.Constants.Storage.DATA
import one.mixin.android.Constants.Storage.IMAGE
import one.mixin.android.Constants.Storage.TRANSCRIPT
import one.mixin.android.Constants.Storage.VIDEO
import one.mixin.android.MixinApplication
import one.mixin.android.extension.fileStats
import one.mixin.android.extension.getStorageTypePath
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.TranscriptDeleteJob
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.ui.common.message.CleanMessageHelper
import one.mixin.android.util.SINGLE_DB_THREAD
import one.mixin.android.vo.ConversationStorageUsage
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.StorageUsage
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingStorageViewModel
    @Inject
    internal constructor(
        private val conversationRepository: ConversationRepository,
        private val cleanMessageHelper: CleanMessageHelper,
        private val jobManager: MixinJobManager,
    ) : ViewModel() {
        private val mediaTypes = listOf(IMAGE, VIDEO, AUDIO, DATA)

        suspend fun getStorageUsage(
            context: Context,
            conversationId: String,
        ): List<StorageUsage> =
            withContext(Dispatchers.IO) {
                val result =
                    mediaTypes.map { type ->
                        async {
                            val stats = context.getStorageTypePath(type)?.let { File(it, conversationId).fileStats() } ?: return@async null
                            if (stats.count == 0 || stats.size == 0L) return@async null
                            StorageUsage(conversationId, type, stats.count, stats.size.toKb())
                        }
                    }.awaitAll().filterNotNull().toMutableList()
                val transcriptSize = conversationRepository.getMediaSizeTotalById(conversationId) ?: 0L
                if (transcriptSize > 0) {
                    result.add(StorageUsage(conversationId, TRANSCRIPT, conversationRepository.countTranscriptById(conversationId), transcriptSize.toKb()))
                }
                result
            }

        suspend fun getConversationStorageUsage(context: Context): List<ConversationStorageUsage> =
            withContext(Dispatchers.IO) {
                val conversations = async { conversationRepository.getConversationStorageUsage() }
                // Scan each media root once and group by the conversation sub-directory.
                val fileSizes = HashMap<String, Long>()
                mediaTypes.map { type ->
                    async {
                        context.getStorageTypePath(type)?.listFiles()
                            ?.filter { it.isDirectory }
                            ?.map { it.name to it.fileStats().size }
                            .orEmpty()
                    }
                }.awaitAll().forEach { sizes ->
                    sizes.forEach { (conversationId, size) -> fileSizes.merge(conversationId, size, Long::plus) }
                }
                conversations.await()
                    .onEach { item ->
                        item.mediaSize = ((fileSizes[item.conversationId] ?: 0L) + (item.transcriptSize ?: 0L)).toKb()
                    }.filter { item ->
                        item.mediaSize != 0L && item.conversationId.isNotEmpty()
                    }.sortedByDescending { item ->
                        item.mediaSize
                    }
            }

        suspend fun clear(
            conversationId: String,
            types: Collection<String>,
        ) = withContext(Dispatchers.IO) {
            types.forEach { type ->
                when (type) {
                    IMAGE -> clearMedia(conversationId, type, MessageCategory.SIGNAL_IMAGE.name, MessageCategory.PLAIN_IMAGE.name, MessageCategory.ENCRYPTED_IMAGE.name)
                    VIDEO -> clearMedia(conversationId, type, MessageCategory.SIGNAL_VIDEO.name, MessageCategory.PLAIN_VIDEO.name, MessageCategory.ENCRYPTED_VIDEO.name)
                    AUDIO -> clearMedia(conversationId, type, MessageCategory.SIGNAL_AUDIO.name, MessageCategory.PLAIN_AUDIO.name, MessageCategory.ENCRYPTED_AUDIO.name)
                    DATA -> clearMedia(conversationId, type, MessageCategory.SIGNAL_DATA.name, MessageCategory.PLAIN_DATA.name, MessageCategory.ENCRYPTED_DATA.name)
                    TRANSCRIPT -> {
                        val ids = conversationRepository.findTranscriptIdByConversationId(conversationId)
                        if (ids.isNotEmpty()) {
                            jobManager.addJobInBackground(TranscriptDeleteJob(ids))
                        }
                    }
                }
            }
            conversationRepository.refreshConversationById(conversationId)
        }

        private suspend fun clearMedia(
            conversationId: String,
            type: String,
            signalCategory: String,
            plainCategory: String,
            encryptedCategory: String,
        ) {
            MixinApplication.appContext.getStorageTypePath(type)?.let { File(it, conversationId) }?.deleteRecursively()
            val list = conversationRepository.getMediaByConversationIdAndCategory(conversationId, signalCategory, plainCategory, encryptedCategory)
            if (list.isEmpty()) return
            withContext(SINGLE_DB_THREAD) {
                cleanMessageHelper.deleteMessageMinimals(conversationId, list)
            }
        }

        private fun Long.toKb() = (this + 1023) / 1024
    }
