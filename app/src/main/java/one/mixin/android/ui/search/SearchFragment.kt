package one.mixin.android.ui.search

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.di.Injectable
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.util.onlyLast
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.ConversationItemMinimal
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class SearchFragment : BaseFragment(), Injectable {

    private lateinit var searchContext: CoroutineContext
    private lateinit var searchContactChannel: Channel<Deferred<List<User>?>>
    private lateinit var searchAssetChannel: Channel<Deferred<List<AssetItem>?>>
    private lateinit var searchUserChannel: Channel<Deferred<List<User>?>>
    private lateinit var searchChatChannel: Channel<Deferred<List<ChatMinimal>?>>
    private lateinit var searchGroupChannel: Channel<Deferred<List<ConversationItemMinimal>?>>
    private lateinit var searchMessageChannel: Channel<Deferred<List<SearchMessageItem>?>>

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
        set(value) {
            if (field != value) {
                field = value
                bindData()
            }
        }

    fun setQueryText(text: String) {
        if (isAdded && text != keyword) {
            keyword = text
        }
    }

    private var searchDisposable: Disposable? = null
    @Suppress("UNCHECKED_CAST")
    @SuppressLint("CheckResult")
    private fun bindData(keyword: String? = this@SearchFragment.keyword) {
        searchDisposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
        fuzzySearch(keyword)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_search, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        searchContext = Job()
        searchContactChannel = Channel()
        searchAssetChannel = Channel()
        searchUserChannel = Channel()
        searchGroupChannel = Channel()
        searchChatChannel = Channel()
        searchMessageChannel = Channel()
        search_rv.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        search_rv.addItemDecoration(StickyRecyclerHeadersDecoration(searchAdapter))
        search_rv.adapter = searchAdapter

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

            @SuppressLint("CheckResult")
            override fun onMessageClick(message: SearchMessageItem) {
                searchViewModel.findConversationById(message.conversationId).subscribe {
                    search_rv.hideKeyboard()
                    ConversationActivity.show(context!!,
                        conversationId = message.conversationId,
                        messageId = message.messageId,
                        keyword = keyword)
                }
            }

            override fun onChatClick(chatMinimal: ChatMinimal) {
                search_rv.hideKeyboard()
                context?.let { ctx -> ConversationActivity.show(ctx, chatMinimal.conversationId) }
            }

            override fun onUserClick(user: User) {
                search_rv.hideKeyboard()
                context?.let { ctx -> ConversationActivity.show(ctx, null, user.userId) }
            }
        }
        setContactListener {
            searchAdapter.setDefaultData(it)
        }
        setAssetListener {
            searchAdapter.setAssetData(it)
        }
        setUserListener {
            searchAdapter.setUserData(it)
        }
        setGroupListener {
            searchAdapter.setGroupData(it)
        }
        setChatListener {
            searchAdapter.setChatData(it)
        }
        setMessageListener {
            searchAdapter.setMessageData(it)
        }
        bindData()
    }

    override fun onDetach() {
        super.onDetach()
        searchContactChannel.close()
        searchAssetChannel.close()
        searchUserChannel.close()
        searchGroupChannel.close()
        searchChatChannel.close()
        searchMessageChannel.close()
        searchContext.cancelChildren()
    }

    private fun fuzzySearch(keyword: String?) = runBlocking(searchContext) {
        searchContactChannel.send(searchViewModel.contactList(keyword))
        searchAssetChannel.send(searchViewModel.fuzzySearchAsset(keyword))
        searchUserChannel.send(searchViewModel.fuzzySearchUser(keyword))
        searchGroupChannel.send(searchViewModel.fuzzySearchGroup(keyword))
        searchChatChannel.send(searchViewModel.fuzzySearchChat(keyword))
        searchMessageChannel.send(searchViewModel.fuzzySearchMessage(keyword))
    }

    private fun setContactListener(listener: (List<User>?) -> Unit) = GlobalScope.launch(searchContext) {
        for (result in onlyLast(searchContactChannel)) {
            withContext(Dispatchers.Main) {
                listener(result)
            }
        }
    }

    private fun setAssetListener(userListener: (List<AssetItem>?) -> Unit) = GlobalScope.launch(searchContext) {
        for (result in onlyLast(searchAssetChannel)) {
            withContext(Dispatchers.Main) {
                userListener(result)
            }
        }
    }

    private fun setUserListener(listener: (List<User>?) -> Unit) = GlobalScope.launch(searchContext) {
        for (result in onlyLast(searchUserChannel)) {
            withContext(Dispatchers.Main) {
                listener(result)
            }
        }
    }

    private fun setGroupListener(userListener: (List<ConversationItemMinimal>?) -> Unit) = GlobalScope.launch(searchContext) {
        for (result in onlyLast(searchGroupChannel)) {
            withContext(Dispatchers.Main) {
                userListener(result)
            }
        }
    }

    private fun setChatListener(chatListener: (List<ChatMinimal>?) -> Unit) = GlobalScope.launch(searchContext) {
        for (result in onlyLast(searchChatChannel)) {
            withContext(Dispatchers.Main) {
                chatListener(result)
            }
        }
    }

    private fun setMessageListener(listener: (List<SearchMessageItem>?) -> Unit) = GlobalScope.launch(searchContext) {
        for (result in onlyLast(searchMessageChannel)) {
            withContext(Dispatchers.Main) {
                listener(result)
            }
        }
    }

    interface OnSearchClickListener {
        fun onUserClick(user: User)
        fun onChatClick(chatMinimal: ChatMinimal)
        fun onGroupClick(conversationItemMinimal: ConversationItemMinimal)
        fun onMessageClick(message: SearchMessageItem)
        fun onAsset(assetItem: AssetItem)
    }
}