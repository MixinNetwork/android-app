package one.mixin.android.ui.search

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
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
    private val userRepository: UserRepository,
    private val conversationRepository: ConversationRepository,
    private val assetRepository: AssetRepository
) : ViewModel() {
    var contactList = userRepository.findFriends()
    fun fuzzySearchUser(query: String): List<User> =
        userRepository.fuzzySearchUser("%${query.trim()}%")

    fun fuzzySearchMessage(query: String): List<SearchMessageItem> =
        conversationRepository.fuzzySearchMessage("%${query.trim()}%")

    fun fuzzySearchAsset(query: String): List<AssetItem> =
        assetRepository.fuzzySearchAsset("%${query.trim()}%")

    fun fuzzySearchGroup(query: String): List<ConversationItemMinimal> =
        conversationRepository.fuzzySearchGroup("%${query.trim()}%")

    fun findConversationById(conversationId: String): Observable<Conversation> =
        conversationRepository.findConversationById(conversationId)
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
}