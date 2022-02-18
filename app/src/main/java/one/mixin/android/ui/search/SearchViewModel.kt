package one.mixin.android.ui.search

import android.os.CancellationSignal
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Config
import androidx.paging.PagedList
import androidx.paging.toLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.Constants.PAGE_SIZE
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
import javax.inject.Inject

@HiltViewModel
class SearchViewModel
@Inject
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

    suspend inline fun <reified T> fuzzySearch(cancellationSignal: CancellationSignal, query: String?, limit: Int = -1): List<Parcelable>? =
        if (query.isNullOrBlank()) {
            null
        } else {
            val escapedQuery = query.trim().escapeSql()
            when (T::class) {
                AssetItem::class -> assetRepository.fuzzySearchAsset(escapedQuery, cancellationSignal)
                User::class -> userRepository.fuzzySearchUser(escapedQuery, cancellationSignal)
                ChatMinimal::class -> conversationRepository.fuzzySearchChat(escapedQuery, cancellationSignal)
                else -> messageControlledRunner.cancelPreviousThenRun {
                    conversationRepository.fuzzySearchMessage(escapedQuery, limit, cancellationSignal)
                }
            }
        }

    fun findAppsByIds(appIds: List<String>) = userRepository.findAppsByIds(appIds)

    fun observeFuzzySearchMessageDetail(
        query: String,
        conversationId: String,
        cancellationSignal: CancellationSignal,
    ): LiveData<PagedList<SearchMessageDetailItem>> {
        val escapedQuery = query.trim().escapeSql()
        return conversationRepository.fuzzySearchMessageDetail(escapedQuery, conversationId, cancellationSignal).toLiveData(
            config = Config(
                pageSize = PAGE_SIZE,
                prefetchDistance = PAGE_SIZE * 2,
                enablePlaceholders = false,
            )
        )
    }

    fun search(query: String): Observable<MixinResponse<User>> =
        accountRepository.search(query).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    fun insertUser(user: User) = viewModelScope.launch(Dispatchers.IO) {
        userRepository.upsert(user)
    }

    suspend fun findMessageIndex(conversationId: String, messageId: String) =
        conversationRepository.findMessageIndex(conversationId, messageId)
}
