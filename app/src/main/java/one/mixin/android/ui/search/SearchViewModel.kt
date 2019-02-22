package one.mixin.android.ui.search

import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import one.mixin.android.repository.AssetRepository
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationItemMinimal
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

    fun contactList(query: String?): Deferred<List<User>?> = GlobalScope.async {
        if (query.isNullOrBlank()) {
            userRepository.syncFindFriends()
        } else {
            null
        }
    }

    fun fuzzySearchUser(query: String?): Deferred<List<User>?> =
        GlobalScope.async {
            if (query.isNullOrBlank()) {
                null
            } else {
                userRepository.fuzzySearchUser("%${query.trim()}%")
            }
        }

    fun fuzzySearchMessage(query: String?): Deferred<List<SearchMessageItem>?> =
        GlobalScope.async {
            if (query.isNullOrBlank()) {
                null
            } else {
                conversationRepository.fuzzySearchMessage("%${query.trim()}%")
            }
        }

    fun fuzzySearchAsset(query: String?): Deferred<List<AssetItem>?> = GlobalScope.async {
        if (query.isNullOrBlank()) {
            null
        } else {
            assetRepository.fuzzySearchAsset("%${query.trim()}%")
        }
    }

    fun fuzzySearchGroup(query: String?): Deferred<List<ConversationItemMinimal>?> =
        GlobalScope.async {
            if (query.isNullOrBlank()) {
                null
            } else {
                conversationRepository.fuzzySearchGroup("%${query.trim()}%")
            }
        }
}