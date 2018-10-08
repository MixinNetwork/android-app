package one.mixin.android.ui.search

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.cancelChildren
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import one.mixin.android.R
import one.mixin.android.di.Injectable
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.util.onlyLast
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ConversationItemMinimal
import one.mixin.android.vo.SearchDataPackage
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import org.jetbrains.anko.support.v4.onUiThread
import javax.inject.Inject
import kotlin.coroutines.experimental.CoroutineContext

class SearchFragment : BaseFragment(), Injectable {

    private lateinit var searchContext: CoroutineContext
    private lateinit var searchChannel: Channel<Deferred<SearchDataPackage>>

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
        fuzzySearch(searchContext, keyword)
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
        searchChannel = Channel()
        search_rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
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

            override fun onUserClick(user: User) {
                search_rv.hideKeyboard()
                context?.let { ctx -> ConversationActivity.show(ctx, null, user.userId) }
            }
        }
        setSearchListener {
            onUiThread {
                if (it.contactList != null) {
                    searchAdapter.setData(null, it.contactList, null, null)
                } else {
                    searchAdapter.setData(it.assetList, it.userList, it.groupList, it.messageList)
                }
            }
        }
        bindData()
    }

    override fun onDetach() {
        super.onDetach()
        searchChannel.close()
        searchContext.cancelChildren()
    }

    private fun fuzzySearch(context: CoroutineContext, keyword: String?) = runBlocking(context) {
        searchChannel.send(searchViewModel.fuzzySearch(keyword))
    }

    private fun setSearchListener(listener: (SearchDataPackage) -> Unit) = GlobalScope.launch(searchContext) {
        for (result in onlyLast(searchChannel)) {
            listener(result)
        }
    }

    interface OnSearchClickListener {
        fun onUserClick(user: User)
        fun onGroupClick(conversationItemMinimal: ConversationItemMinimal)
        fun onMessageClick(message: SearchMessageItem)
        fun onAsset(assetItem: AssetItem)
    }
}