package one.mixin.android.ui.search

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding3.widget.textChanges
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.FragmentSearchSingleBinding
import one.mixin.android.databinding.ViewHeadSearchSingleBinding
import one.mixin.android.event.SearchEvent
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getParcelableArrayListCompat
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.search.SearchFragment.Companion.SEARCH_DEBOUNCE
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.wallet.WalletActivity.Destination
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.Dapp
import one.mixin.android.vo.MaoUser
import one.mixin.android.vo.RecentSearch
import one.mixin.android.vo.RecentSearchType
import one.mixin.android.vo.SearchBot
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.safe.TokenItem
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class SearchSingleFragment : BaseFragment(R.layout.fragment_search_single) {
    companion object {
        const val TAG = "SearchSingleFragment"
        const val ARGS_LIST = "args_list"
        const val ARGS_QUERY = "args_query"

        fun newInstance(
            list: ArrayList<Parcelable>,
            query: String,
        ) = SearchSingleFragment().withArgs {
            putParcelableArrayList(ARGS_LIST, list)
            putString(ARGS_QUERY, query)
        }
    }

    private val searchViewModel by viewModels<SearchViewModel>()

    private val data by lazy {
        requireArguments().getParcelableArrayListCompat(ARGS_LIST, Parcelable::class.java)
    }

    private val query by lazy {
        requireArguments().getString(ARGS_QUERY)!!
    }

    private val type by lazy {
        when (data!![0]) {
            is TokenItem -> TypeAsset
            is ChatMinimal -> TypeChat
            is User -> TypeUser
            is Dapp -> TypeDapp
            is Market -> TypeMarket
            is SearchBot -> TypeBot
            else -> TypeMessage
        }
    }

    private val adapter by lazy {
        SearchSingleAdapter(type).apply { query = this@SearchSingleFragment.query }
    }

    private val binding by viewBinding(FragmentSearchSingleBinding::bind)

    private var searchJob: Job? = null
    private var cancellationSignal: CancellationSignal? = null
    private lateinit var searchChatPopupMenu: SearchChatPopupMenu

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        searchChatPopupMenu =
            SearchChatPopupMenu(requireActivity(), lifecycleScope, searchViewModel) {
                onTextChanged(binding.searchEt.text.toString())
            }
        binding.backIb.setOnClickListener {
            binding.searchEt.hideKeyboard()
            requireActivity().onBackPressed()
        }
        binding.searchRv.layoutManager = LinearLayoutManager(requireContext())
        val header = LayoutInflater.from(requireContext()).inflate(R.layout.view_head_search_single, binding.searchRv, false)
        val headerBinding = ViewHeadSearchSingleBinding.bind(header)
        val text =
            when (type) {
                TypeAsset -> requireContext().getString(R.string.ASSETS)
                TypeUser -> requireContext().getText(R.string.CONTACTS)
                TypeChat -> requireContext().getText(R.string.CHATS)
                TypeMessage -> requireContext().getText(R.string.SEARCH_MESSAGES)
                TypeMarket -> requireContext().getString(R.string.ASSETS)
                TypeDapp -> requireContext().getString(R.string.DAPPS)
                TypeBot -> requireContext().getString(R.string.BOTS)
                else -> ""
            }
        headerBinding.titleTv.text = text
        adapter.headerView = header
        binding.searchRv.adapter = adapter
        adapter.data = data
        adapter.onItemClickListener =
            object : SearchFragment.OnSearchClickListener {
                override fun onTipClick() {
                    // do noting
                }

                override fun onUrlClick(url: String) {
                    // do noting
                }

                override fun onAssetClick(tokenItem: TokenItem) {
                    activity?.let { WalletActivity.showWithToken(it, tokenItem, Destination.Transactions) }
                }

                override fun onMarketClick(market: Market) {
                    lifecycleScope.launch {
                        searchViewModel.findMarketItemByCoinId(market.coinId)?.let { marketItem ->
                            searchViewModel.saveRecentSearch(requireContext().defaultSharedPreferences, RecentSearch(RecentSearchType.MARKET, iconUrl = marketItem.iconUrl, title = marketItem.symbol, primaryKey = marketItem.coinId))
                            RxBus.publish(SearchEvent())
                            WalletActivity.showWithMarket(requireActivity(), marketItem, Destination.Market)
                        }
                    }
                }

                override fun onDappClick(dapp: Dapp) {
                    searchViewModel.saveRecentSearch(requireContext().defaultSharedPreferences, RecentSearch(RecentSearchType.DAPP, iconUrl = dapp.iconUrl, title = dapp.name, subTitle = dapp.homeUrl))
                    RxBus.publish(SearchEvent())
                    WebActivity.show(requireContext(), dapp.homeUrl, null)
                }

                override fun onBotClick(bot: SearchBot) {
                    val f = UserBottomSheetDialogFragment.newInstance(bot.toUser())
                    searchViewModel.saveRecentSearch(requireContext().defaultSharedPreferences, RecentSearch(RecentSearchType.BOT, iconUrl = bot.avatarUrl, title = bot.fullName, subTitle = bot.identityNumber, primaryKey = bot.appId))
                    RxBus.publish(SearchEvent())
                    f?.show(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
                }

                override fun onMessageClick(message: SearchMessageItem) {
                    binding.searchRv.hideKeyboard()
                    val f = SearchMessageFragment.newInstance(message, adapter.query)
                    requireActivity().addFragment(this@SearchSingleFragment, f, SearchMessageFragment.TAG, R.id.container)
                }

                override fun onChatClick(chatMinimal: ChatMinimal) {
                    binding.searchRv.hideKeyboard()
                    context?.let { ctx -> ConversationActivity.show(ctx, chatMinimal.conversationId) }
                }

                override fun onUserClick(user: User) {
                    binding.searchRv.hideKeyboard()
                    context?.let { ctx -> ConversationActivity.show(ctx, null, user.userId) }
                }

                override fun onUserClick(user: MaoUser) {
                    // do nothing
                }

                override fun onMaoAppClick(userId: String) {
                    // do nothing
                }

                override fun onChatLongClick(
                    chatMinimal: ChatMinimal,
                    anchor: View,
                ): Boolean {
                    searchChatPopupMenu.showPopupMenu(chatMinimal, anchor)
                    return true
                }
            }

        binding.clearIb.setOnClickListener { binding.searchEt.setText("") }
        binding.searchEt.hint = text
        binding.searchEt.setText(query)
        binding.searchEt.textChanges().debounce(SEARCH_DEBOUNCE, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe(
                {
                    binding.clearIb.isVisible = it.isNotEmpty()
                    if (it == adapter.query) {
                        binding.pb.isInvisible = true
                        return@subscribe
                    }

                    adapter.query = it.toString()
                    searchJob?.cancel()
                    cancellationSignal?.cancel()
                    searchJob = onTextChanged(it.toString())
                },
                {},
            )
    }

    override fun onDestroy() {
        super.onDestroy()
        cancellationSignal?.cancel()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun onTextChanged(s: String) =
        lifecycleScope.launch {
            if (viewDestroyed()) return@launch

            binding.pb.isVisible = true

            val cancellationSignal = CancellationSignal()
            this@SearchSingleFragment.cancellationSignal = cancellationSignal
            val list: List<Parcelable>? =
                when (type) {
                    TypeAsset -> searchViewModel.fuzzySearch<TokenItem>(cancellationSignal, s)
                    TypeUser -> searchViewModel.fuzzySearch<User>(cancellationSignal, s)
                    TypeChat -> searchViewModel.fuzzySearch<ChatMinimal>(cancellationSignal, s)
                    TypeMessage -> searchViewModel.fuzzySearch<SearchMessageItem>(cancellationSignal, s, -1)
                    TypeMarket -> searchViewModel.fuzzySearch<Market>(cancellationSignal, s, -1)
                    TypeDapp -> searchViewModel.fuzzySearch<Dapp>(cancellationSignal, s, -1)
                    TypeBot -> searchViewModel.fuzzySearch<SearchBot>(cancellationSignal, s, -1)
                    else -> throw IllegalArgumentException("Unknown type: $type")
                }

            binding.pb.isInvisible = true

            adapter.data = list
            adapter.notifyDataSetChanged()
        }
}
