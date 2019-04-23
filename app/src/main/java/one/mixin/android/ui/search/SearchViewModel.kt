package one.mixin.android.ui.search

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import one.mixin.android.repository.AssetRepository
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.SearchMessageDetailItem
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import javax.inject.Inject

class SearchViewModel @Inject
internal constructor(
    val userRepository: UserRepository,
    val conversationRepository: ConversationRepository,
    val assetRepository: AssetRepository
) : ViewModel() {

    fun findConversationById(conversationId: String): Observable<Conversation> =
        conversationRepository.findConversationById(conversationId)
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    inline fun <reified T> fuzzySearchAsync(query: String?, limit: Int = 4): Deferred<List<Parcelable>?> =
        viewModelScope.async(Dispatchers.Default) {
            if (query.isNullOrBlank()) {
                null
            } else {
                when (T::class) {
                    AssetItem::class -> assetRepository.fuzzySearchAsset("%${query.trim()}%")
                    User::class -> userRepository.fuzzySearchUser("%${query.trim()}%")
                    ChatMinimal::class -> conversationRepository.fuzzySearchChat("%${query.trim()}%")
                    else -> conversationRepository.fuzzySearchMessage("%${query.trim()}%", limit)
                }
            }
        }

    fun findAppsByIds(appIds: List<String>) = userRepository.findAppsByIds(appIds)

    fun fuzzySearchMessageDetailAsync(query: String, conversationId: String): Deferred<LiveData<PagedList<SearchMessageDetailItem>>> =
        viewModelScope.async(Dispatchers.Default) {
            LivePagedListBuilder(conversationRepository.fuzzySearchMessageDetail("%${query.trim()}%", conversationId),
                PagedList.Config.Builder()
                    .setPageSize(30)
                    .setEnablePlaceholders(true)
                    .build())
                .build()
        }
}