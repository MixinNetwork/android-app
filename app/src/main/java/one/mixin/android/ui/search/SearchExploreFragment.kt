package one.mixin.android.ui.search

import android.os.Bundle
import android.os.CancellationSignal
import android.os.Parcelable
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.widget.textChanges
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersTouchListener
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.FragmentSearchExploreBinding
import one.mixin.android.databinding.ItemSearchHeaderBinding
import one.mixin.android.event.SearchEvent
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.openAsUrlOrWeb
import one.mixin.android.extension.showKeyboard
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.UserBottomSheetDialogFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.search.components.RecentSearchPage
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.wallet.WalletActivity.Destination
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.Dapp
import one.mixin.android.vo.RecentSearch
import one.mixin.android.vo.RecentSearchType
import one.mixin.android.vo.SearchBot
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import one.mixin.android.vo.market.Market
import one.mixin.android.vo.safe.TokenItem
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class SearchExploreFragment : BaseFragment(R.layout.fragment_search_explore) {
    private val searchViewModel by viewModels<SearchViewModel>()

    private val searchAdapter: SearchExploreAdapter by lazy {
        SearchExploreAdapter()
    }

    companion object {
        const val TAG = "SearchExploreFragment"
        const val SEARCH_DEBOUNCE = 300L
    }

    private var keyword: String? = null
        set(value) {
            if (field != value) {
                field = value
                bindData(value)
            }
        }

    private fun setQueryText(text: String) {
        if (isAdded && text != keyword) {
            searchAdapter.query = text
            keyword = text
        }
    }

    private val decoration by lazy { StickyRecyclerHeadersDecoration(searchAdapter) }

    private val binding by viewBinding(FragmentSearchExploreBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Trigger ViewModel initialization without executing any code
        searchViewModel
        view.setOnClickListener {
            if (keyword.isNullOrBlank()) {
                (requireActivity() as MainActivity).closeSearch()
            }
        }
        binding.searchRv.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.searchRv.adapter = searchAdapter
        binding.searchRv.addItemDecoration(decoration)
        binding.searchRv.addOnItemTouchListener(
            StickyRecyclerHeadersTouchListener(binding.searchRv, decoration).apply {
                setOnHeaderClickListener { headerView, position, _, e ->

                    if (ItemSearchHeaderBinding.bind(headerView).searchHeaderMore.x > e.rawX) return@setOnHeaderClickListener

                    searchAdapter.getTypeData(position)?.let {
                        val f =
                            SearchSingleFragment.newInstance(
                                arrayListOf<Parcelable>().apply {
                                    addAll(it)
                                },
                                keyword ?: "",
                            )
                        requireActivity().addFragment(this@SearchExploreFragment, f, SearchSingleFragment.TAG, R.id.container)
                        binding.searchRv.hideKeyboard()
                    }
                }
            },
        )
        binding.backIb.setOnClickListener {
            binding.searchEt.et.hideKeyboard()
            requireActivity().onBackPressed()
        }
        lifecycleScope.launch {
            delay(200)
            if (isAdded) {
                binding.searchEt.et.showKeyboard()
            }
        }

        searchAdapter.onItemClickListener =
            object : SearchFragment.OnSearchClickListener {
                override fun onUserClick(user: User) {
                    // do noting
                }

                override fun onBotClick(bot: SearchBot) {
                    val f = UserBottomSheetDialogFragment.newInstance(bot.toUser())
                    searchViewModel.saveRecentSearch(requireContext().defaultSharedPreferences, RecentSearch(RecentSearchType.BOT, iconUrl = bot.avatarUrl, title = bot.fullName, subTitle = bot.identityNumber, primaryKey = bot.appId))
                    f?.show(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
                }

                override fun onChatClick(chatMinimal: ChatMinimal) {
                    // do noting
                }

                override fun onMessageClick(message: SearchMessageItem) {
                    // do noting
                }

                override fun onAssetClick(tokenItem: TokenItem) {
                    // do noting
                }

                override fun onDappClick(dapp: Dapp) {
                    searchViewModel.saveRecentSearch(requireContext().defaultSharedPreferences, RecentSearch(RecentSearchType.DAPP, iconUrl = dapp.iconUrl, title = dapp.name, subTitle = dapp.homeUrl))
                    WebActivity.show(requireContext(), dapp.homeUrl, null)
                }

                override fun onTipClick() {
                    // do noting
                }

                override fun onMarketClick(market: Market) {
                    lifecycleScope.launch {
                        searchViewModel.findMarketItemByCoinId(market.coinId)?.let { marketItem ->
                            searchViewModel.saveRecentSearch(requireContext().defaultSharedPreferences, RecentSearch(RecentSearchType.MARKET, iconUrl = marketItem.iconUrl, title = marketItem.symbol, primaryKey = marketItem.coinId))
                            WalletActivity.showWithMarket(requireActivity(), marketItem, Destination.Market)
                        }
                    }
                }

                override fun onUrlClick(url: String) {
                    url.openAsUrlOrWeb(requireContext(), null, parentFragmentManager, lifecycleScope, saveName = true)
                }

                override fun onChatLongClick(chatMinimal: ChatMinimal, anchor: View): Boolean {
                    // do noting
                    return false
                }
            }

        binding.searchEt.et.textChanges().debounce(SEARCH_DEBOUNCE, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe(
                {
                    setQueryText(it.toString())
                },
                {},
            )
        binding.recent.setContent {
            RecentSearchPage ({ dapp ->
                searchViewModel.saveRecentSearch(requireContext().defaultSharedPreferences, RecentSearch(RecentSearchType.DAPP, iconUrl = dapp.iconUrl, title = dapp.name, subTitle = dapp.homeUrl))
                WebActivity.show(requireContext(), dapp.homeUrl, null)
            }, {search->
                when(search.type){
                    RecentSearchType.BOT-> {
                        lifecycleScope.launch {
                            searchViewModel.findUserByAppId(search.primaryKey!!)?.let { user ->
                                val f = UserBottomSheetDialogFragment.newInstance(user)
                                f?.show(parentFragmentManager, UserBottomSheetDialogFragment.TAG)
                            }
                        }
                    }
                    RecentSearchType.DAPP->{
                        WebActivity.show(requireContext(), search.subTitle?:"", null)
                    }
                    RecentSearchType.LINK->{
                        search.subTitle?.openAsUrlOrWeb(requireContext(), null, parentFragmentManager, lifecycleScope)
                    }
                    RecentSearchType.MARKET->{
                        lifecycleScope.launch {
                            searchViewModel.findMarketItemByCoinId(search.primaryKey!!)?.let { marketItem ->
                                WalletActivity.showWithMarket(requireActivity(), marketItem, Destination.Market)
                            }
                        }
                    }
                }
            })
        }
        binding.va.displayedChild = 2
        lifecycleScope.launch {
            fuzzySearch(null)
        }
        RxBus.listen(SearchEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe {
                lifecycleScope.launch {
                    searchViewModel.getRecentSearch(requireContext().defaultSharedPreferences)
                }
            }
    }

    private var searchJob: Job? = null
    private var searchUrlJob: Job? = null
    private var searchMarketsJob: Job? = null
    private var searchBotsJob: Job? = null
    private var searchDappsJob: Job? = null

    @Suppress("UNCHECKED_CAST")
    private fun bindData(keyword: String?) {
        searchJob?.cancel()
        searchUrlJob?.cancel()
        searchMarketsJob?.cancel()
        searchBotsJob?.cancel()
        searchDappsJob?.cancel()
        searchJob = fuzzySearch(keyword)
    }

    private fun fuzzySearch(keyword: String?) =
        lifecycleScope.launch {
            if (viewDestroyed()) return@launch
            if (keyword.isNullOrBlank()) {
                binding.va.displayedChild = 2
                return@launch
            } else {
                binding.va.displayedChild = 1
            }

            val cancellationSignal = CancellationSignal()

            searchUrlJob =
                launch {
                    searchViewModel.fuzzySearchUrl(keyword).let { url ->
                        searchAdapter.setUrlData(url)
                    }
                    updateRv(searchUrlJob)
                }

            searchBotsJob =
                launch {
                    searchViewModel.fuzzyBots(cancellationSignal, keyword).let { bots ->
                        searchAdapter.setBots(bots)
                    }
                    updateRv(searchBotsJob)
                }

            searchMarketsJob =
                launch {
                    searchViewModel.fuzzyMarkets(cancellationSignal, keyword).let { markets ->
                        searchAdapter.setMarkets(markets)
                    }
                    updateRv(searchMarketsJob)
                }

            searchDappsJob =
                launch {
                    searchViewModel.getAllDapps().filter { dapp ->
                        dapp.name.contains(keyword) || dapp.homeUrl.contains(keyword)
                    }.let { dapps ->
                        searchAdapter.setDapps(dapps)
                    }
                    updateRv(searchDappsJob)
                }
        }

    private fun allJobIsCompleted(job: Job?): Boolean {
        return listOf(searchUrlJob, searchMarketsJob, searchBotsJob, searchDappsJob).filter {
            job != it
        }.all { it?.isCompleted == true }
    }

    private fun updateRv(job: Job?) {
        if (allJobIsCompleted(job)) {
            if (searchAdapter.itemCount == 0) {
                binding.va.displayedChild = 0
            } else {
                binding.va.displayedChild = 1
            }
        }
    }
}
