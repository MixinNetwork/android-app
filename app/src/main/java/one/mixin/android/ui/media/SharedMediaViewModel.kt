package one.mixin.android.ui.media

import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.cachedIn
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
import one.mixin.android.vo.isAppCard
import one.mixin.android.vo.isSignal
import one.mixin.android.vo.isVideo
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class SharedMediaViewModel
    @Inject
    constructor(
        val conversationRepository: ConversationRepository,
        private val jobManager: MixinJobManager,
    ) : ViewModel() {

        fun getMediaMessagesExcludeLive(conversationId: String): LiveData<PagingData<MessageItem>> {
            val liveData = MutableLiveData<PagingData<MessageItem>>()
            viewModelScope.launch(Dispatchers.IO) {
                val list = conversationRepository.getMediaMessagesExcludeLiveList(conversationId)
                val filteredList = list.filter { !it.isAppCard() || it.isAppCardWithCover() }
                liveData.postValue(PagingData.from(filteredList))
            }
            return liveData
        }

        fun getAudioMessages(conversationId: String): LiveData<PagingData<MessageItem>> {
            val liveData = MutableLiveData<PagingData<MessageItem>>()
            viewModelScope.launch(Dispatchers.IO) {
                val list = conversationRepository.getAudioMessagesList(conversationId)
                val sortedList = list.sortedWith(
                    Comparator<MessageItem> { o1, o2 ->
                        if (o1 == null || o2 == null) return@Comparator 0

                        val time1 = ZonedDateTime.parse(o1.createdAt)
                        val time2 = ZonedDateTime.parse(o2.createdAt)
                        val year1 = time1.year
                        val year2 = time2.year
                        val day1 = time1.dayOfYear
                        val day2 = time2.dayOfYear
                        if (year1 == year2) {
                            if (day1 == day2) {
                                return@Comparator time1.toOffsetDateTime()
                                    .compareTo(time2.toOffsetDateTime())
                            } else {
                                return@Comparator day2 - day1
                            }
                        } else {
                            return@Comparator year2 - year1
                        }
                    },
                )
                liveData.postValue(PagingData.from(sortedList))
            }
            return liveData
        }

        fun getPostMessages(conversationId: String): LiveData<PagingData<MessageItem>> =
            pager { conversationRepository.getPostMessages(conversationId) }

        fun getLinkMessages(conversationId: String): LiveData<PagingData<HyperlinkItem>> =
            pager { conversationRepository.getLinkMessages(conversationId) }

        fun getFileMessages(conversationId: String): LiveData<PagingData<MessageItem>> =
            pager { conversationRepository.getFileMessages(conversationId) }

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

    @OptIn(UnstableApi::class)
    fun getMediaMessages(
        conversationId: String,
        index: Int,
        excludeLive: Boolean,
    ): LiveData<PagingData<MessageItem>> =
        pager(
            pageSize = MediaPagerActivity.PAGE_SIZE,
            initialKey = index,
        ) {
            conversationRepository.getMediaMessagesPagingSource(conversationId, excludeLive)
        }

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

    private fun <T : Any> pager(
        pageSize: Int = PAGE_SIZE,
        initialKey: Int? = null,
        pagingSourceFactory: () -> PagingSource<Int, T>,
    ): LiveData<PagingData<T>> =
        Pager(
            config =
                PagingConfig(
                    pageSize = pageSize,
                    prefetchDistance = pageSize * 2,
                    enablePlaceholders = true,
                ),
            initialKey = initialKey,
            pagingSourceFactory = pagingSourceFactory,
        ).flow.cachedIn(viewModelScope).asLiveData()
}
