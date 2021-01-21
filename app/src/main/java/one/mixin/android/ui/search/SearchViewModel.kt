package one.mixin.android.ui.search

import android.os.Parcelable
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import one.mixin.android.api.MixinResponse
import one.mixin.android.extension.escapeSql
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.AssetRepository
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.util.ControlledRunner
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.SearchMessageDetailItem
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User

class SearchViewModel @ViewModelInject
internal constructor(
    val userRepository: UserRepository,
    val conversationRepository: ConversationRepository,
    val assetRepository: AssetRepository,
    val accountRepository: AccountRepository
) : ViewModel() {

    val messageControlledRunner = ControlledRunner<List<SearchMessageItem>?>()

    fun findConversationById(conversationId: String): Observable<Conversation> =
        conversationRepository.findConversationById(conversationId)
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    suspend inline fun <reified T> fuzzySearch(query: String?, limit: Int = -1): List<Parcelable>? =
        if (query.isNullOrBlank()) {
            null
        } else {
            val escapedQuery = query.trim().escapeSql()
            when (T::class) {
                AssetItem::class -> assetRepository.fuzzySearchAsset(escapedQuery)
                User::class -> userRepository.fuzzySearchUser(escapedQuery)
                ChatMinimal::class -> conversationRepository.fuzzySearchChat(escapedQuery)
                else -> messageControlledRunner.cancelPreviousThenRun {
                    conversationRepository.fuzzySearchMessage(escapedQuery, limit)
                }
            }
        }

    fun findAppsByIds(appIds: List<String>) = userRepository.findAppsByIds(appIds)

    suspend fun fuzzySearchMessageDetailAsync(
        query: String,
        conversationId: String
    ): LiveData<PagedList<SearchMessageDetailItem>> {
        val escapedQuery = query.trim().escapeSql()
        return LivePagedListBuilder(
            withContext(Dispatchers.IO) {
                conversationRepository.fuzzySearchMessageDetail(escapedQuery, conversationId)
            },
            PagedList.Config.Builder()
                .setPageSize(30)
                .setEnablePlaceholders(true)
                .build()
        )
            .build()
    }

    fun search(query: String): Observable<MixinResponse<User>> =
        accountRepository.search(query).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    fun insertUser(user: User) = viewModelScope.launch(Dispatchers.IO) {
        userRepository.upsert(user)
    }
}
