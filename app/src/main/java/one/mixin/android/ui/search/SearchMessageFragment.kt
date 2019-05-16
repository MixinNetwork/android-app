package one.mixin.android.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.viewModelScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding3.widget.textChanges
import com.uber.autodispose.autoDisposable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_search_message.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.search.SearchFragment.Companion.SEARCH_DEBOUNCE
import one.mixin.android.ui.search.SearchSingleFragment.Companion.ARGS_QUERY
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.SearchMessageDetailItem
import one.mixin.android.vo.SearchMessageItem
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SearchMessageFragment : BaseFragment() {
    companion object {
        const val TAG = "SearchMessageFragment"
        const val ARGS_SEARCH_MESSAGE = "args_search_message"

        fun newInstance(
            searchMessageItem: SearchMessageItem,
            query: String
        ) = SearchMessageFragment().withArgs {
            putParcelable(ARGS_SEARCH_MESSAGE, searchMessageItem)
            putString(ARGS_QUERY, query)
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val searchViewModel: SearchViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(SearchViewModel::class.java)
    }

    private val searchMessageItem: SearchMessageItem by lazy {
        arguments!!.getParcelable<SearchMessageItem>(ARGS_SEARCH_MESSAGE)
    }
    private val query by lazy { arguments!!.getString(ARGS_QUERY) }

    private val adapter by lazy { SearchMessageAdapter() }

    private var observer: Observer<PagedList<SearchMessageDetailItem>>? = null
    private var curLiveData: LiveData<PagedList<SearchMessageDetailItem>>? = null

    private var compositeDisposable = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_search_message, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener {
            search_rv.hideKeyboard()
            requireActivity().onBackPressed()
        }
        title_view.avatar_iv.visibility = VISIBLE
        title_view.avatar_iv.setTextSize(16f)
        title_view.avatar_iv.setInfo(searchMessageItem.conversationName,
            searchMessageItem.conversationAvatarUrl, searchMessageItem.conversationId)
        if (searchMessageItem.conversationCategory == ConversationCategory.CONTACT.name) {
            title_view.setSubTitle(searchMessageItem.userFullName
                ?: "", getString(R.string.search_related_message, searchMessageItem.messageCount))
            title_view.avatar_iv.setInfo(searchMessageItem.userFullName,
                searchMessageItem.userAvatarUrl, searchMessageItem.userId)
        } else {
            title_view.setSubTitle(searchMessageItem.conversationName
                ?: "", getString(R.string.search_related_message, searchMessageItem.messageCount))
            title_view.avatar_iv.setGroup(searchMessageItem.conversationAvatarUrl)
        }

        search_rv.layoutManager = LinearLayoutManager(requireContext())
        adapter.callback = object : SearchMessageAdapter.SearchMessageCallback {
            override fun onItemClick(item: SearchMessageDetailItem) {
                searchViewModel.findConversationById(searchMessageItem.conversationId)
                    .autoDisposable(scopeProvider)
                    .subscribe {
                        search_et.hideKeyboard()
                        ConversationActivity.show(requireContext(),
                            conversationId = searchMessageItem.conversationId,
                            messageId = item.messageId,
                            keyword = search_et.text.toString())
                    }
            }
        }
        search_rv.adapter = adapter

        clear_ib.setOnClickListener { search_et.setText("") }
        search_et.setText(query)
        compositeDisposable.add(search_et.textChanges().debounce(SEARCH_DEBOUNCE, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                clear_ib.isVisible = it.isNotEmpty()
                onTextChanged(it.toString())
            }, {}))
        search_et.postDelayed({
            onTextChanged(query)
        }, 50)
    }

    override fun onDetach() {
        super.onDetach()
        compositeDisposable.dispose()
    }

    private fun onTextChanged(s: String) {
        if (s == adapter.query) return

        adapter.query = s
        if (s.isEmpty()) {
            observer?.let {
                curLiveData?.removeObserver(it)
            }
            observer = null
            curLiveData = null
            adapter.submitList(null)
            return
        }

        searchViewModel.viewModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                observer?.let {
                    curLiveData?.removeObserver(it)
                }
            }
            curLiveData = searchViewModel.fuzzySearchMessageDetailAsync(s, searchMessageItem.conversationId).await()
            withContext(Dispatchers.Main) {
                observer = Observer {
                    adapter.submitList(it)
                }
                observer?.let {
                    curLiveData?.observe(this@SearchMessageFragment, it)
                }
            }
        }
    }
}