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
import org.threeten.bp.ZonedDateTime

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
        val dataSource = conversationRepository.getAudioMessages(conversationId)
        val sortedDataSource = dataSource.mapByPage { list ->
            list.sortWith(Comparator<MessageItem> { o1, o2 ->
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
            })
            return@mapByPage list
        }
        return LivePagedListBuilder(
            sortedDataSource,
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
