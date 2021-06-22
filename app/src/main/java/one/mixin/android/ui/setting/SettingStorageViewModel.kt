package one.mixin.android.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.Storage.AUDIO
import one.mixin.android.Constants.Storage.DATA
import one.mixin.android.Constants.Storage.IMAGE
import one.mixin.android.Constants.Storage.TRANSCRIPT
import one.mixin.android.Constants.Storage.VIDEO
import one.mixin.android.MixinApplication
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getConversationAudioPath
import one.mixin.android.extension.getConversationDocumentPath
import one.mixin.android.extension.getConversationImagePath
import one.mixin.android.extension.getConversationMediaSize
import one.mixin.android.extension.getConversationVideoPath
import one.mixin.android.extension.getStorageUsageByConversationAndType
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.TranscriptDeleteJob
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.util.SINGLE_DB_THREAD
import one.mixin.android.vo.ConversationStorageUsage
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.StorageUsage
import javax.inject.Inject

@HiltViewModel
class SettingStorageViewModel
@Inject
internal constructor(
    private val conversationRepository: ConversationRepository,
    private val jobManager: MixinJobManager
) : ViewModel() {

    fun getStorageUsage(conversationId: String): Single<List<StorageUsage>> =
        Single.just(conversationId).map { cid ->
            val result = mutableListOf<StorageUsage>()
            val context = MixinApplication.appContext
            context.getStorageUsageByConversationAndType(cid, IMAGE)?.apply {
                result.add(this)
            }
            context.getStorageUsageByConversationAndType(cid, VIDEO)?.apply {
                result.add(this)
            }
            context.getStorageUsageByConversationAndType(cid, AUDIO)?.apply {
                result.add(this)
            }
            context.getStorageUsageByConversationAndType(cid, DATA)?.apply {
                result.add(this)
            }
            conversationRepository.getMediaSizeTotalById(cid)?.apply {
                result.add(StorageUsage(conversationId, TRANSCRIPT, conversationRepository.countTranscriptById(cid), this))
            }
            result.toList()
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun getConversationStorageUsage(): Flowable<List<ConversationStorageUsage>> = conversationRepository.getConversationStorageUsage()
        .map { list ->
            list.asSequence().map { item ->
                val context = MixinApplication.appContext
                item.mediaSize = context.getConversationMediaSize(item.conversationId)
                item
            }.filter { conversationStorageUsage ->
                conversationStorageUsage.mediaSize != 0L && conversationStorageUsage.conversationId.isNotEmpty()
            }.sortedByDescending { conversationStorageUsage ->
                conversationStorageUsage.mediaSize
            }.toList()
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    fun clear(conversationId: String, type: String) {
        if (MixinApplication.appContext.defaultSharedPreferences.getBoolean(Constants.Account.PREF_ATTACHMENT, false)) {
            when (type) {
                IMAGE -> {
                    MixinApplication.get().getConversationImagePath(conversationId)?.deleteRecursively()
                    conversationRepository.deleteMediaMessageByConversationAndCategory(
                        conversationId,
                        MessageCategory.SIGNAL_IMAGE.name,
                        MessageCategory.PLAIN_IMAGE.name
                    )
                }
                VIDEO -> {
                    MixinApplication.get().getConversationVideoPath(conversationId)?.deleteRecursively()
                    conversationRepository.deleteMediaMessageByConversationAndCategory(
                        conversationId,
                        MessageCategory.SIGNAL_VIDEO.name,
                        MessageCategory.PLAIN_VIDEO.name
                    )
                }
                AUDIO -> {
                    MixinApplication.get().getConversationAudioPath(conversationId)?.deleteRecursively()
                    conversationRepository.deleteMediaMessageByConversationAndCategory(
                        conversationId,
                        MessageCategory.SIGNAL_AUDIO.name,
                        MessageCategory.PLAIN_AUDIO.name
                    )
                }
                DATA -> {
                    MixinApplication.get().getConversationDocumentPath(conversationId)?.deleteRecursively()
                    conversationRepository.deleteMediaMessageByConversationAndCategory(
                        conversationId,
                        MessageCategory.SIGNAL_DATA.name,
                        MessageCategory.PLAIN_DATA.name
                    )
                }
                TRANSCRIPT -> {
                    conversationRepository.deleteMediaMessageByConversationAndCategory(
                        conversationId,
                        MessageCategory.SIGNAL_TRANSCRIPT.name,
                        MessageCategory.PLAIN_TRANSCRIPT.name
                    )
                }
            }
        } else {
            when (type) {
                IMAGE -> clear(conversationId, MessageCategory.SIGNAL_IMAGE.name, MessageCategory.PLAIN_IMAGE.name)
                VIDEO -> clear(conversationId, MessageCategory.SIGNAL_VIDEO.name, MessageCategory.PLAIN_VIDEO.name)
                AUDIO -> clear(conversationId, MessageCategory.SIGNAL_AUDIO.name, MessageCategory.PLAIN_AUDIO.name)
                DATA -> clear(conversationId, MessageCategory.SIGNAL_DATA.name, MessageCategory.PLAIN_DATA.name)
                TRANSCRIPT -> clear(conversationId, MessageCategory.SIGNAL_TRANSCRIPT.name, MessageCategory.PLAIN_TRANSCRIPT.name)
            }
        }
        conversationRepository.refreshConversationById(conversationId)
    }

    private fun clear(conversationId: String, signalCategory: String, plainCategory: String) {
        if (signalCategory == MessageCategory.SIGNAL_TRANSCRIPT.name && plainCategory == MessageCategory.PLAIN_TRANSCRIPT.name) {
            viewModelScope.launch(SINGLE_DB_THREAD) {
                val ids = conversationRepository.findTranscriptIdByConversationId(conversationId)
                if (ids.isEmpty()) {
                    return@launch
                }
                jobManager.addJobInBackground(TranscriptDeleteJob(ids))
            }
            return
        }
        conversationRepository.getMediaByConversationIdAndCategory(conversationId, signalCategory, plainCategory)
            ?.let { list ->
                list.forEach { item ->
                    conversationRepository.deleteMessage(item.messageId, item.mediaUrl)
                }
            }
    }
}
