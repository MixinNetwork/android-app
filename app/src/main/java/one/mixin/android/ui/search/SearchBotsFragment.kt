package one.mixin.android.ui.search

import android.os.Bundle
import android.os.CancellationSignal
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.widget.textChanges
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants.Account.PREF_RECENT_USED_BOTS
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSearchBotsBinding
import one.mixin.android.databinding.ItemSearchAppBinding
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.deserialize
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.openAsUrlOrWeb
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.common.profile.ProfileBottomSheetDialogFragment
import one.mixin.android.ui.common.showUserBottom
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.RecentUsedApp
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import one.mixin.android.vo.safe.TokenItem
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class SearchBotsFragment : BaseFragment(R.layout.fragment_search_bots) {
    private val searchViewModel by viewModels<SearchViewModel>()

    private val searchAdapter: SearchAdapter by lazy {
        SearchAdapter()
    }

    companion object {
        const val TAG = "SearchBotsFragment"
        const val SEARCH_DEBOUNCE = 300L
    }

    private var keyword: String? = null
        set(value) {
            if (field != value) {
                field = value
                bindData()
            }
        }

    private fun setQueryText(text: String) {
        if (isAdded && text != keyword) {
            if (text.isNotBlank()) {
                showSearch()
            } else {
                showBots()
            }

            searchAdapter.query = text
            keyword = text
        }
    }

    private var searchJob: Job? = null

    @Suppress("UNCHECKED_CAST")
    private fun bindData(keyword: String? = this@SearchBotsFragment.keyword) {
        searchJob?.cancel()
        searchJob = fuzzySearch(keyword)
    }

    private val appAdapter = AppAdapter()

    private val binding by viewBinding(FragmentSearchBotsBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        view.setOnClickListener {
            if (keyword.isNullOrBlank()) {
                (requireActivity() as MainActivity).closeSearch()
            }
        }
        binding.searchRv.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.searchRv.adapter = searchAdapter

        binding.appRv.layoutManager = GridLayoutManager(requireContext(), 4)
        appAdapter.appListener =
            object : AppListener {
                override fun onItemClick(app: RecentUsedApp) {
                    (requireActivity() as MainActivity).closeSearch()
                    ConversationActivity.show(requireContext(), null, app.appId)
                }
            }
        binding.appRv.adapter = appAdapter

        showBots()

        searchAdapter.onItemClickListener =
            object : SearchFragment.OnSearchClickListener {
                override fun onTipClick() {
                    binding.searchRv.hideKeyboard()
                    searchAdapter.searchingId = true
                    searchViewModel.search(searchAdapter.query).autoDispose(stopScope).subscribe(
                        { r ->
                            searchAdapter.searchingId = false
                            when {
                                r.isSuccess ->
                                    r.data?.let { data ->
                                        if (data.userId == Session.getAccountId()) {
                                            ProfileBottomSheetDialogFragment.newInstance().showNow(
                                                parentFragmentManager,
                                                UserBottomSheetDialogFragment.TAG,
                                            )
                                        } else {
                                            searchViewModel.insertUser(user = data)
                                            showUserBottom(parentFragmentManager, data)
                                        }
                                    }

                                r.errorCode == ErrorHandler.NOT_FOUND -> toast(R.string.User_not_found)
                                else -> ErrorHandler.handleMixinError(r.errorCode, r.errorDescription)
                            }
                        },
                        { t: Throwable ->
                            searchAdapter.searchingId = false
                            ErrorHandler.handleError(t)
                        },
                    )
                }

                override fun onUrlClick(url: String) {
                    url.openAsUrlOrWeb(requireContext(), null, parentFragmentManager, lifecycleScope)
                }

                override fun onAsset(tokenItem: TokenItem) {
                    activity?.let { WalletActivity.showWithToken(it, tokenItem, WalletActivity.Destination.Transactions) }
                }

                override fun onMessageClick(message: SearchMessageItem) {
                    binding.searchRv.hideKeyboard()
                    val f = SearchMessageFragment.newInstance(message, keyword ?: "")
                    requireActivity().addFragment(this@SearchBotsFragment, f, SearchMessageFragment.TAG, R.id.container)
                }

                override fun onChatClick(chatMinimal: ChatMinimal) {
                    binding.searchRv.hideKeyboard()
                    context?.let { ctx -> ConversationActivity.show(ctx, chatMinimal.conversationId) }
                }

                override fun onUserClick(user: User) {
                    binding.searchRv.hideKeyboard()
                    context?.let { ctx -> ConversationActivity.show(ctx, null, user.userId) }
                }

                override fun onChatLongClick(
                    chatMinimal: ChatMinimal,
                    anchor: View,
                ): Boolean {
                    binding.searchRv.hideKeyboard()
                    return true
                }
            }
        bindData()

        binding.searchEt.textChanges().debounce(SEARCH_DEBOUNCE, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(stopScope)
            .subscribe(
                {
                    setQueryText(it.toString())
                },
                {},
            )
    }

    override fun onResume() {
        super.onResume()
        loadRecentUsedApps()
    }

    private fun loadRecentUsedApps() =
        lifecycleScope.launch {
            if (viewDestroyed()) return@launch

            val apps =
                withContext(Dispatchers.IO) {
                    var botsList =
                        defaultSharedPreferences.getString(PREF_RECENT_USED_BOTS, null)?.split("=")
                            ?: return@withContext null
                    if (botsList.size == 1 && !botsList[0].isUUID()) {
                        getPreviousVersionBotsList()?.let {
                            botsList = it
                        }
                    }
                    if (botsList.isEmpty()) return@withContext null
                    val result = searchViewModel.findAppsByIds(botsList.take(8))
                    if (result.isEmpty()) return@withContext null
                    result.sortedBy {
                        botsList.indexOf(it.appId)
                    }
                }
            if (apps.isNullOrEmpty()) return@launch

            appAdapter.submitList(apps)
        }

    private fun getPreviousVersionBotsList(): List<String>? {
        defaultSharedPreferences.getString(PREF_RECENT_USED_BOTS, null)?.let { botsString ->
            return botsString.deserialize<Array<String>>()?.toList()
        } ?: return null
    }

    private fun showSearch() {
        binding.searchRv.post {
            if (viewDestroyed()) return@post

            binding.searchRv.isVisible = true
            binding.appRv.isGone = true
        }
    }

    private fun showBots() {
        binding.searchRv.post {
            if (viewDestroyed()) return@post

            binding.searchRv.isGone = true
            binding.appRv.isVisible = true
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun fuzzySearch(keyword: String?) =
        lifecycleScope.launch {
            if (viewDestroyed()) return@launch

            searchAdapter.clear()

            val cancellationSignal = CancellationSignal()

            val users = searchViewModel.fuzzyBots(cancellationSignal, keyword) as List<User>?
            searchAdapter.setData(null, users, null)

            (requireActivity() as MainActivity).hideSearchLoading()
        }


    internal class AppAdapter : ListAdapter<RecentUsedApp, AppHolder>(RecentUsedApp.DIFF_CALLBACK) {
        var appListener: AppListener? = null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ) =
            AppHolder(ItemSearchAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(
            holder: AppHolder,
            position: Int,
        ) {
            getItem(position)?.let {
                holder.bind(it, appListener)
            }
        }
    }

    internal class AppHolder(val binding: ItemSearchAppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            app: RecentUsedApp,
            listener: AppListener?,
        ) {
            binding.iconIv.setInfo(app.fullName, app.iconUrl, app.appId)
            binding.nameTv.text = app.fullName
            binding.root.setOnClickListener {
                listener?.onItemClick(app)
            }
        }
    }

    interface AppListener {
        fun onItemClick(app: RecentUsedApp)
    }
}
