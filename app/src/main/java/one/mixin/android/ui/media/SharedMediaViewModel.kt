package one.mixin.android.ui.media

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.Constants.PAGE_SIZE
import one.mixin.android.job.AttachmentDownloadJob
import one.mixin.android.job.ConvertVideoJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SendAttachmentMessageJob
import one.mixin.android.job.SendGiphyJob
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.ui.media.pager.MediaPagerActivity
import one.mixin.android.vo.EncryptCategory
import one.mixin.android.vo.HyperlinkItem
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isEncrypted
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isSignal
import one.mixin.android.vo.isVideo
import javax.inject.Inject

@HiltViewModel
class SharedMediaViewModel
    @Inject
    constructor(
        val conversationRepository: ConversationRepository,
        private val jobManager: MixinJobManager,
    ) : ViewModel() {

        fun getMediaMessagesExcludeLive(conversationId: String): LiveData<PagedList<MessageItem>> =
            LivePagedListBuilder(
                conversationRepository.getMediaMessagesExcludeLive(conversationId),
                PagedList.Config.Builder()
                    .setPrefetchDistance(MediaFragment.PAGE_SIZE * 2)
                    .setPageSize(MediaFragment.PAGE_SIZE)
                    .setEnablePlaceholders(true)
                    .build(),
            )
                .build()

        fun getAudioMessages(conversationId: String): LiveData<PagedList<MessageItem>> =
            LivePagedListBuilder(
                conversationRepository.getAudioMessages(conversationId),
                PagedList.Config.Builder()
                    .setPrefetchDistance(PAGE_SIZE * 2)
                    .setPageSize(PAGE_SIZE)
                    .setEnablePlaceholders(true)
                    .build(),
            )
                .build()

        fun getPostMessages(conversationId: String): LiveData<PagedList<MessageItem>> {
            return LivePagedListBuilder(
                conversationRepository.getPostMessages(conversationId),
                PagedList.Config.Builder()
                    .setPrefetchDistance(PAGE_SIZE * 2)
                    .setPageSize(PAGE_SIZE)
                    .setEnablePlaceholders(true)
                    .build(),
            )
                .build()
        }

        fun getLinkMessages(conversationId: String): LiveData<PagedList<HyperlinkItem>> {
            return LivePagedListBuilder(
                conversationRepository.getLinkMessages(conversationId),
                PagedList.Config.Builder()
                    .setPrefetchDistance(PAGE_SIZE * 2)
                    .setPageSize(PAGE_SIZE)
                    .setEnablePlaceholders(true)
                    .build(),
            )
                .build()
        }

        fun getFileMessages(conversationId: String): LiveData<PagedList<MessageItem>> {
            return LivePagedListBuilder(
                conversationRepository.getFileMessages(conversationId),
                PagedList.Config.Builder()
                    .setPrefetchDistance(PAGE_SIZE * 2)
                    .setPageSize(PAGE_SIZE)
                    .setEnablePlaceholders(true)
                    .build(),
            )
                .build()
        }

        fun retryDownload(id: String) =
            viewModelScope.launch(Dispatchers.IO) {
                conversationRepository.findMessageById(id)?.let {
                    jobManager.addJobInBackground(AttachmentDownloadJob(it))
                }
            }

        fun retryUpload(
            id: String,
            onError: () -> Unit,
        ) =
            viewModelScope.launch(Dispatchers.IO) {
                conversationRepository.findMessageById(id)?.let {
                    if (it.isVideo() && it.mediaSize != null && it.mediaSize == 0L) {
                        try {
                            jobManager.addJobInBackground(
                                ConvertVideoJob(
                                    it.conversationId,
                                    it.userId,
                                    Uri.parse(it.mediaUrl),
                                    0f,
                                    1f,
                                    when {
                                        it.isSignal() -> EncryptCategory.SIGNAL
                                        it.isEncrypted() -> EncryptCategory.ENCRYPTED
                                        else -> EncryptCategory.PLAIN
                                    },
                                    it.messageId,
                                    it.createdAt,
                                ),
                            )
                        } catch (e: NullPointerException) {
                            onError.invoke()
                        }
                    } else if (it.isImage() && it.mediaSize != null && it.mediaSize == 0L) { // un-downloaded GIPHY
                        val category =
                            when {
                                it.isSignal() -> MessageCategory.SIGNAL_IMAGE
                                it.isEncrypted() -> MessageCategory.ENCRYPTED_IMAGE
                                else -> MessageCategory.PLAIN_IMAGE
                            }.name
                        try {
                            jobManager.addJobInBackground(
                                SendGiphyJob(
                                    it.conversationId,
                                    it.userId,
                                    it.mediaUrl!!,
                                    it.mediaWidth!!,
                                    it.mediaHeight!!,
                                    it.mediaSize,
                                    category,
                                    it.messageId,
                                    it.thumbImage ?: "",
                                    it.createdAt,
                                ),
                            )
                        } catch (e: NullPointerException) {
                            onError.invoke()
                        }
                    } else {
                        jobManager.addJobInBackground(SendAttachmentMessageJob(it))
                    }
                }
            }

        fun cancel(
            id: String,
            conversationId: String,
        ) =
            viewModelScope.launch(Dispatchers.IO) {
                jobManager.cancelJobByMixinJobId(id) {
                    viewModelScope.launch {
                        conversationRepository.updateMediaStatusSuspend(MediaStatus.CANCELED.name, id, conversationId)
                    }
                }
            }

        suspend fun indexMediaMessages(
            conversationId: String,
            messageId: String,
            excludeLive: Boolean,
        ): Int = conversationRepository.indexMediaMessages(conversationId, messageId, excludeLive)

        suspend fun countIndexMediaMessages(
            conversationId: String,
            excludeLive: Boolean,
        ) = conversationRepository.countIndexMediaMessages(conversationId, excludeLive)

    fun getMediaMessages(
        conversationId: String,
        index: Int,
        excludeLive: Boolean,
    ): LiveData<PagedList<MessageItem>> = conversationRepository.getMediaMessages(conversationId, index, excludeLive)

    suspend fun getMediaMessage(
        conversationId: String,
        messageId: String,
    ) =
        conversationRepository.getMediaMessage(conversationId, messageId)

    fun downloadByMessageId(messageId: String) =
        viewModelScope.launch {
            conversationRepository.suspendFindMessageById(messageId)?.let {
                jobManager.addJobInBackground(AttachmentDownloadJob(it))
            }
        }
}
