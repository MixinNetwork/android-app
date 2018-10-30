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
import one.mixin.android.vo.SearchDataPackage
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class SearchViewModel @Inject
internal constructor(
    private val userRepository: UserRepository,
    private val conversationRepository: ConversationRepository,
    private val assetRepository: AssetRepository
) : ViewModel() {

    fun findConversationById(conversationId: String): Observable<Conversation> =
        conversationRepository.findConversationById(conversationId)
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

    private fun contactList() = userRepository.syncFindFriends()

    private fun fuzzySearchUser(context: CoroutineContext, query: String): Deferred<List<User>> =
        GlobalScope.async(context) {
            userRepository.fuzzySearchUser("%${query.trim()}%")
        }

    private fun fuzzySearchMessage(context: CoroutineContext, query: String): Deferred<List<SearchMessageItem>> =
        GlobalScope.async(context) {
            conversationRepository.fuzzySearchMessage("%${query.trim()}%")
        }

    private fun fuzzySearchAsset(context: CoroutineContext, query: String): Deferred<List<AssetItem>> = GlobalScope.async(context) {
        assetRepository.fuzzySearchAsset("%${query.trim()}%")
    }

    private fun fuzzySearchGroup(context: CoroutineContext, query: String): Deferred<List<ConversationItemMinimal>> =
        GlobalScope.async(context) {
            conversationRepository.fuzzySearchGroup("%${query.trim()}%")
        }

    fun fuzzySearch(keyword: String?) = GlobalScope.async {
        if (keyword.isNullOrBlank()) {
            SearchDataPackage(contactList(), null, null, null, null)
        } else {
            val assetList = fuzzySearchAsset(coroutineContext, keyword!!).await()
            val userList = fuzzySearchUser(coroutineContext, keyword).await()
            val groupList = fuzzySearchGroup(coroutineContext, keyword).await()
            val messageList = fuzzySearchMessage(coroutineContext, keyword).await()
            SearchDataPackage(null, assetList, userList, groupList, messageList)
        }
    }
}