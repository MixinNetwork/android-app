package one.mixin.android.ui.search

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding3.widget.textChanges
import kotlinx.android.synthetic.main.fragment_search_message.*
import kotlinx.android.synthetic.main.view_title.view.*
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_search_message, container, false)

    @SuppressLint("CheckResult")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        title_view.left_ib.setOnClickListener { requireActivity().onBackPressed() }
        title_view.avatar_iv.visibility = VISIBLE
        title_view.avatar_iv.setTextSize(16f)
        title_view.avatar_iv.setInfo(searchMessageItem.conversationName, searchMessageItem.conversationAvatarUrl, searchMessageItem.conversationId)
        if (searchMessageItem.conversationCategory == ConversationCategory.CONTACT.name) {
            title_view.setSubTitle(searchMessageItem.userFullName
                ?: "", getString(R.string.search_related_message, searchMessageItem.messageCount))
            title_view.avatar_iv.setInfo(searchMessageItem.userFullName, searchMessageItem.userAvatarUrl, searchMessageItem.userId)
        } else {
            title_view.setSubTitle(searchMessageItem.conversationName
                ?: "", getString(R.string.search_related_message, searchMessageItem.messageCount))
            title_view.avatar_iv.setGroup(searchMessageItem.conversationAvatarUrl)
        }

        search_rv.layoutManager = LinearLayoutManager(requireContext())
        adapter.callback = object : SearchMessageAdapter.SearchMessageCallback {
            override fun onItemClick(item: SearchMessageDetailItem) {
                searchViewModel.findConversationById(searchMessageItem.conversationId).subscribe {
                    search_et.hideKeyboard()
                    ConversationActivity.show(requireContext(),
                        conversationId = searchMessageItem.conversationId,
                        messageId = item.messageId,
                        keyword = search_et.text.toString())
                }
            }
        }
        search_rv.adapter = adapter

        search_et.textChanges().debounce(300, TimeUnit.MILLISECONDS).subscribe {
            onTextChanged(it.toString())
        }
        search_et.setText(query)
    }

    private fun onTextChanged(s: String) {
        if (s == adapter.query) return

        adapter.query = s
        if (s.isEmpty()) {
            adapter.submitList(null)
            return
        }

        searchViewModel.viewModelScope.launch {
            searchViewModel.fuzzySearchMessageDetailAsync(s, searchMessageItem.conversationId).await()
                .observe(this@SearchMessageFragment, Observer {
                    adapter.submitList(it)
                })
        }
    }
}