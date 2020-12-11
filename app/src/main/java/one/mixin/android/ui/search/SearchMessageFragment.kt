package one.mixin.android.ui.search

import android.os.Bundle
import android.view.View
import android.view.View.VISIBLE
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding3.widget.textChanges
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSearchMessageBinding
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.search.SearchFragment.Companion.SEARCH_DEBOUNCE
import one.mixin.android.ui.search.SearchSingleFragment.Companion.ARGS_QUERY
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.SearchMessageDetailItem
import one.mixin.android.vo.SearchMessageItem
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class SearchMessageFragment : BaseFragment(R.layout.fragment_search_message) {
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

    private val searchViewModel by viewModels<SearchViewModel>()

    private val searchMessageItem: SearchMessageItem by lazy {
        requireArguments().getParcelable(ARGS_SEARCH_MESSAGE)!!
    }
    private val query by lazy { requireArguments().getString(ARGS_QUERY)!! }

    private val adapter by lazy { SearchMessageAdapter() }

    private var observer: Observer<PagedList<SearchMessageDetailItem>>? = null
    private var curLiveData: LiveData<PagedList<SearchMessageDetailItem>>? = null

    private val binding by viewBinding(FragmentSearchMessageBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener {
            binding.searchRv.hideKeyboard()
            requireActivity().onBackPressed()
        }
        binding.titleView.avatarIv.visibility = VISIBLE
        binding.titleView.avatarIv.setTextSize(16f)
        if (searchMessageItem.conversationCategory == ConversationCategory.CONTACT.name) {
            binding.titleView.titleTv.text = searchMessageItem.userFullName
            binding.titleView.avatarIv.setInfo(
                searchMessageItem.userFullName,
                searchMessageItem.userAvatarUrl,
                searchMessageItem.userId
            )
        } else {
            binding.titleView.titleTv.text = searchMessageItem.conversationName
            binding.titleView.avatarIv.setGroup(searchMessageItem.conversationAvatarUrl)
        }

        binding.searchRv.layoutManager = LinearLayoutManager(requireContext())
        adapter.callback = object : SearchMessageAdapter.SearchMessageCallback {
            override fun onItemClick(item: SearchMessageDetailItem) {
                searchViewModel.findConversationById(searchMessageItem.conversationId)
                    .autoDispose(stopScope)
                    .subscribe {
                        binding.searchEt.hideKeyboard()
                        ConversationActivity.show(
                            requireContext(),
                            conversationId = searchMessageItem.conversationId,
                            messageId = item.messageId,
                            keyword = binding.searchEt.text.toString()
                        )
                        if (isConversationSearch()) {
                            parentFragmentManager.popBackStack()
                        }
                    }
            }
        }
        binding.searchRv.adapter = adapter

        binding.clearIb.setOnClickListener { binding.searchEt.setText("") }
        binding.searchEt.setText(query)
        binding.searchEt.textChanges().debounce(SEARCH_DEBOUNCE, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe(
                {
                    binding.clearIb.isVisible = it.isNotEmpty()
                    onTextChanged(it.toString())
                },
                {}
            )
        binding.searchEt.postDelayed(
            {
                onTextChanged(query)
            },
            50
        )
        if (isConversationSearch()) {
            binding.searchEt.postDelayed(
                {
                    binding.searchEt.showKeyboard()
                },
                500
            )
        }
    }

    private fun isConversationSearch() = searchMessageItem.messageCount == 0

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

        lifecycleScope.launch {
            if (!isAdded) return@launch

            observer?.let {
                curLiveData?.removeObserver(it)
            }
            curLiveData = searchViewModel.fuzzySearchMessageDetailAsync(s, searchMessageItem.conversationId)
            observer = Observer {
                adapter.submitList(it)
            }
            observer?.let {
                curLiveData?.observe(viewLifecycleOwner, it)
            }
        }
    }
}
