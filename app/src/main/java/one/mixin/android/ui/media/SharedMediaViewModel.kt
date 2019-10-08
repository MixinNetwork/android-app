package one.mixin.android.ui.media

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import javax.inject.Inject
import one.mixin.android.Constants
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.vo.HyperlinkItem
import one.mixin.android.vo.MessageItem

class SharedMediaViewModel @Inject constructor(
    val conversationRepository: ConversationRepository
) : ViewModel() {

    fun getMediaMessagesExcludeLive(conversationId: String): LiveData<PagedList<MessageItem>> {
        return LivePagedListBuilder(
            conversationRepository.getMediaMessagesExcludeLive(conversationId),
            PagedList.Config.Builder()
                .setPrefetchDistance(Constants.PAGE_SIZE * 2)
                .setPageSize(Constants.PAGE_SIZE)
                .setEnablePlaceholders(true)
                .build()
        )
            .build()
    }

    fun getAudioMessages(conversationId: String): LiveData<PagedList<MessageItem>> {
        return LivePagedListBuilder(
            conversationRepository.getAudioMessages(conversationId),
            PagedList.Config.Builder()
                .setPrefetchDistance(Constants.PAGE_SIZE * 2)
                .setPageSize(Constants.PAGE_SIZE)
                .setEnablePlaceholders(true)
                .build()
        )
            .build()
    }

    fun getLinkMessages(conversationId: String): LiveData<PagedList<HyperlinkItem>> {
        return LivePagedListBuilder(
            conversationRepository.getLinkMessages(conversationId),
            PagedList.Config.Builder()
                .setPrefetchDistance(Constants.PAGE_SIZE * 2)
                .setPageSize(Constants.PAGE_SIZE)
                .setEnablePlaceholders(true)
                .build()
        )
            .build()
    }

    fun getFileMessages(conversationId: String): LiveData<PagedList<MessageItem>> {
        return LivePagedListBuilder(
            conversationRepository.getFileMessages(conversationId),
            PagedList.Config.Builder()
                .setPrefetchDistance(Constants.PAGE_SIZE * 2)
                .setPageSize(Constants.PAGE_SIZE)
                .setEnablePlaceholders(true)
                .build()
        )
            .build()
    }
}
