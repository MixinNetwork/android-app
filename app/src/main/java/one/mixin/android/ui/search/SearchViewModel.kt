package one.mixin.android.ui.search

import android.arch.lifecycle.ViewModel
import io.reactivex.Flowable
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
import java.util.ArrayList
import javax.inject.Inject

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

    private fun fuzzySearchUser(query: String): List<User> =
        userRepository.fuzzySearchUser("%${query.trim()}%")

    private fun fuzzySearchMessage(query: String): List<SearchMessageItem> =
        conversationRepository.fuzzySearchMessage("%${query.trim()}%")

    private fun fuzzySearchAsset(query: String): List<AssetItem> =
        assetRepository.fuzzySearchAsset("%${query.trim()}%")

    private fun fuzzySearchGroup(query: String): List<ConversationItemMinimal> =
        conversationRepository.fuzzySearchGroup("%${query.trim()}%")

    fun fuzzySearch(keyword: String?) = Flowable.just(0)
        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).map {
            if (keyword.isNullOrBlank()) {
                Pair(contactList(), null)
            } else {
                val list = ArrayList<List<Any>>()
                fuzzySearchAsset(keyword!!).let {
                    list.add(it)
                }
                fuzzySearchUser(keyword).let {
                    list.add(it)
                }
                fuzzySearchGroup(keyword).let {
                    list.add(it)
                }
                fuzzySearchMessage(keyword).let {
                    list.add(it)
                }
                Pair(null, list)
            }
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())!!
}