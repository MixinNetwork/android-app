package one.mixin.android.ui.search

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import kotlinx.android.synthetic.main.fragment_search.*
import one.mixin.android.R
import one.mixin.android.di.Injectable
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ConversationItemMinimal
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.runOnUiThread
import javax.inject.Inject

class SearchFragment : Fragment(), Injectable {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val searchViewModel: SearchViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(SearchViewModel::class.java)
    }
    private val searchAdapter: SearchAdapter by lazy {
        SearchAdapter()
    }

    companion object {
        @Volatile
        private var INSTANCE: SearchFragment? = null

        fun getInstance(): SearchFragment =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SearchFragment().also { INSTANCE = it }
            }
    }

    private var keyword: String? = null
    fun setQueryText(text: String) {
        if (isAdded && text != keyword) {
            keyword = text
            if (text.isEmpty()) {
                searchViewModel.contactList.observe(this, Observer {
                    searchAdapter.setData(null, it, null, null)
                })
            } else {
                doAsync {
                    val assetList = searchViewModel.fuzzySearchAsset(text)
                    val userList = searchViewModel.fuzzySearchUser(text)
                    val groupList = searchViewModel.fuzzySearchGroup(text)
                    val messageList = searchViewModel.fuzzySearchMessage(text)
                    context?.runOnUiThread {
                        searchAdapter.setData(assetList, userList, groupList, messageList)
                        if (assetList.isNotEmpty() || userList.isNotEmpty() || groupList.isNotEmpty() || messageList.isNotEmpty()) {
                            search_rv.scrollToPosition(0)
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_search, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        search_rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        search_rv.addItemDecoration(StickyRecyclerHeadersDecoration(searchAdapter))
        search_rv.adapter = searchAdapter
        searchViewModel.contactList.observe(this, Observer {
            searchAdapter.setData(null, it, null, null)
        })
        searchAdapter.onItemClickListener = object : OnSearchClickListener {
            override fun onAsset(assetItem: AssetItem) {
                activity?.let { WalletActivity.show(it, assetItem) }
            }

            override fun onGroupClick(conversationItemMinimal: ConversationItemMinimal) {
                search_rv.hideKeyboard()
                context?.let { ctx ->
                    ConversationActivity.show(ctx, conversationItemMinimal.conversationId, null)
                }
            }

            override fun onMessageClick(message: SearchMessageItem) {
                searchViewModel.findConversationById(message.conversationId).subscribe {
                    search_rv.hideKeyboard()
                    ConversationActivity.show(context!!,
                        conversationId = message.conversationId,
                        messageId = message.messageId,
                        keyword = keyword)
                }
            }

            override fun onUserClick(user: User) {
                search_rv.hideKeyboard()
                context?.let { ctx -> ConversationActivity.show(ctx, null, user.userId) }
            }
        }
    }

    interface OnSearchClickListener {
        fun onUserClick(user: User)
        fun onGroupClick(conversationItemMinimal: ConversationItemMinimal)
        fun onMessageClick(message: SearchMessageItem)
        fun onAsset(assetItem: AssetItem)
    }
}