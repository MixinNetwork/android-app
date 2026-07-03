package one.mixin.android.ui.search

import android.os.Bundle
import android.os.CancellationSignal
import android.view.View
import android.view.View.VISIBLE
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding3.widget.textChanges
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSearchMessageBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.conversation.ConversationFragment
import one.mixin.android.ui.search.SearchFragment.Companion.SEARCH_DEBOUNCE
import one.mixin.android.ui.search.SearchSingleFragment.Companion.ARGS_QUERY
import one.mixin.android.util.analytics.AnalyticsTracker
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.SearchMessageDetailItem
import one.mixin.android.vo.SearchMessageItem
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
@AndroidEntryPoint
class SearchMessageFragment : BaseFragment(R.layout.fragment_search_message) {
    companion object {
        const val TAG = "SearchMessageFragment"
        const val ARGS_SEARCH_MESSAGE = "args_search_message"

        fun newInstance(
            searchMessageItem: SearchMessageItem,
            query: String,
        ) = SearchMessageFragment().withArgs {
            putParcelable(ARGS_SEARCH_MESSAGE, searchMessageItem)
            putString(ARGS_QUERY, query)
        }
    }

    private val searchViewModel by viewModels<SearchViewModel>()

    private val searchMessageItem: SearchMessageItem by lazy {
        requireArguments().getParcelableCompat(ARGS_SEARCH_MESSAGE, SearchMessageItem::class.java)!!
    }

    private val query by lazy { requireArguments().getString(ARGS_QUERY)!! }

    private val adapter by lazy { SearchMessageAdapter() }

    private var observer: Observer<PagingData<SearchMessageDetailItem>>? = null
    private var curLiveData: LiveData<PagingData<SearchMessageDetailItem>>? = null

    private val binding by viewBinding(FragmentSearchMessageBinding::bind)

    private var searchJob: Job? = null
    private var textChangesDisposable: Disposable? = null
    private var searchEditText: EditText? = null

    private var cancellationSignal: CancellationSignal? = null
    private val initialSearchRunnable =
        Runnable {
            if (viewDestroyed()) return@Runnable
            searchJob = onTextChanged(query)
        }
    private val showKeyboardRunnable =
        Runnable {
            if (viewDestroyed()) return@Runnable
            searchEditText?.showKeyboard()
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        searchEditText = binding.searchEt
        binding.titleView.leftIb.setOnClickListener {
            binding.searchRv.hideKeyboard()
            requireActivity().onBackPressed()
        }
        binding.titleView.avatarIv.visibility = VISIBLE
        binding.titleView.avatarIv.setTextSize(16f)
        if (searchMessageItem.conversationCategory == ConversationCategory.CONTACT.name) {
            binding.titleView.titleTv.setTextOnly(searchMessageItem.userFullName)
            binding.titleView.avatarIv.setInfo(
                searchMessageItem.userFullName,
                searchMessageItem.userAvatarUrl,
                searchMessageItem.userId,
            )
        } else {
            binding.titleView.titleTv.setTextOnly(searchMessageItem.conversationName)
            binding.titleView.avatarIv.setGroup(searchMessageItem.conversationAvatarUrl)
        }

        binding.searchRv.layoutManager = LinearLayoutManager(requireContext())
        adapter.callback =
            object : SearchMessageAdapter.SearchMessageCallback {
                override fun onItemClick(item: SearchMessageDetailItem) {
                    val keyword = binding.searchEt.text.toString()
                    if (searchMessageItem.isBot()) {
                        AnalyticsTracker.trackOpenBotConversation(
                            AnalyticsTracker.BotSource.SEARCH_KEY_MESSAGE,
                            searchMessageItem.userIdentityNumber,
                        )
                    }
                    searchViewModel.findConversationById(searchMessageItem.conversationId)
                        .autoDispose(stopScope)
                        .subscribe {
                            if (!viewDestroyed()) {
                                binding.searchEt.hideKeyboard()
                            }
                            val activity = requireActivity()
                            val conversationFragment = activity.supportFragmentManager.findFragmentByTag(ConversationFragment.TAG) as? ConversationFragment
                            if (activity is ConversationActivity && conversationFragment != null) {
                                lifecycleScope.launch {
                                    activity.supportFragmentManager.inTransaction {
                                        setCustomAnimations(R.anim.slide_in_right, 0, 0, R.anim.slide_out_right)
                                        hide(this@SearchMessageFragment)
                                        addToBackStack(null)
                                    }
                                    conversationFragment.updateConversationInfo(item.messageId, keyword)
                                }
                            } else {
                                ConversationActivity.show(
                                    requireContext(),
                                    conversationId = searchMessageItem.conversationId,
                                    messageId = item.messageId,
                                    keyword = keyword,
                                )
                                if (isConversationSearch()) {
                                    parentFragmentManager.popBackStack()
                                }
                            }
                        }
                }
            }
        binding.searchRv.adapter = adapter

        binding.clearIb.setOnClickListener { binding.searchEt.setText("") }
        binding.searchEt.setText(query)
        textChangesDisposable =
            binding.searchEt.textChanges().debounce(SEARCH_DEBOUNCE, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(destroyScope)
                .subscribe(
                {
                    if (viewDestroyed()) return@subscribe
                    binding.clearIb.isVisible = it.isNotEmpty()
                    searchJob?.cancel()
                    searchJob = onTextChanged(it.toString())
                },
                {},
            )
        searchEditText?.postDelayed(initialSearchRunnable, 50)
        if (isConversationSearch()) {
            searchEditText?.postDelayed(showKeyboardRunnable, 500)
        }
    }

    override fun onDestroyView() {
        searchEditText?.removeCallbacks(initialSearchRunnable)
        searchEditText?.removeCallbacks(showKeyboardRunnable)
        searchEditText = null
        textChangesDisposable?.dispose()
        textChangesDisposable = null
        searchJob?.cancel()
        searchJob = null
        removeObserverAndCancel()
        observer = null
        curLiveData = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancellationSignal?.cancel()
    }

    override fun onBackPressed(): Boolean {
        if (requireActivity() is ConversationActivity) {
            val conversationFragment = requireActivity().supportFragmentManager.findFragmentByTag(ConversationFragment.TAG) as? ConversationFragment
            if (conversationFragment != null) {
                requireActivity().supportFragmentManager.inTransaction {
                    conversationFragment.updateConversationInfo(null, null)
                    show(conversationFragment)
                }
            }
        }
        return super.onBackPressed()
    }

    private fun isConversationSearch() = searchMessageItem.messageCount == 0

    private fun onTextChanged(s: String) =
        viewLifecycleOwner.lifecycleScope.launch {
            if (viewDestroyed()) return@launch
            if (s == adapter.query) {
                return@launch
            }

            adapter.query = s
            if (s.isEmpty()) {
                removeObserverAndCancel()
                cancellationSignal = null
                observer?.let {
                    curLiveData?.removeObserver(it)
                }
                observer = null
                curLiveData = null
                binding.progress.isVisible = false
                adapter.submitData(viewLifecycleOwner.lifecycle, PagingData.empty())
                return@launch
            }

            bindAndSearch(s)
        }

    private fun bindAndSearch(s: String) {
        if (viewDestroyed()) return
        binding.progress.isVisible = true

        removeObserverAndCancel()
        cancellationSignal = CancellationSignal()
        curLiveData = searchViewModel.observeFuzzySearchMessageDetail(s, searchMessageItem.conversationId, cancellationSignal!!)
        observer =
            Observer {
                if (viewDestroyed()) return@Observer
                if (s != binding.searchEt.text.toString()) return@Observer
                binding.progress.isVisible = false

                adapter.submitData(viewLifecycleOwner.lifecycle, it)
            }
        observer?.let {
            curLiveData?.observe(viewLifecycleOwner, it)
        }
    }

    private fun removeObserverAndCancel() {
        cancellationSignal?.cancel()
        observer?.let { curLiveData?.removeObserver(it) }
    }
}
