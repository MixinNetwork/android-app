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
import javax.inject.Inject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
import one.mixin.android.vo.User

class SearchViewModel @Inject
internal constructor(
    val userRepository: UserRepository,
    val conversationRepository: ConversationRepository,
    val assetRepository: AssetRepository,
    val accountRepository: AccountRepository
) : ViewModel() {

    val controlledRunner = ControlledRunner<List<Parcelable>?>()

    fun findConversationById(conversationId: String): Observable<Conversation> =
        conversationRepository.findConversationById(conversationId)
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    suspend inline fun <reified T> fuzzySearch(query: String?, limit: Int = -1): List<Parcelable>? =
        if (query.isNullOrBlank()) {
            null
        } else {
            controlledRunner.cancelPreviousThenRun {
                val escapedQuery = query.trim().escapeSql()
                when (T::class) {
                    AssetItem::class -> assetRepository.fuzzySearchAsset("%$escapedQuery%")
                    User::class -> userRepository.fuzzySearchUser("%$escapedQuery%")
                    ChatMinimal::class -> conversationRepository.fuzzySearchChat("%$escapedQuery%")
                    else -> conversationRepository.fuzzySearchMessage("%$escapedQuery%", limit)
                }
            }
        }

    fun findAppsByIds(appIds: List<String>) = userRepository.findAppsByIds(appIds)

    fun fuzzySearchMessageDetailAsync(query: String, conversationId: String): Deferred<LiveData<PagedList<SearchMessageDetailItem>>> =
        viewModelScope.async(Dispatchers.IO) {
            val escapedQuery = query.trim().escapeSql()
            LivePagedListBuilder(conversationRepository.fuzzySearchMessageDetail("%$escapedQuery%", conversationId),
                PagedList.Config.Builder()
                    .setPageSize(30)
                    .setEnablePlaceholders(true)
                    .build())
                .build()
        }

    fun search(query: String): Observable<MixinResponse<User>> =
        accountRepository.search(query).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    fun insertUser(user: User) {
        userRepository.upsert(user)
    }
}
