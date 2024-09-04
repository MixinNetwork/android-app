package one.mixin.android.ui.home.web3

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.PREF_GLOBAL_MARKET
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.FragmentMarketBinding
import one.mixin.android.event.GlobalMarketEvent
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.putInt
import one.mixin.android.extension.screenWidth
import one.mixin.android.extension.translationX
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshGlobalWeb3MarketJob
import one.mixin.android.job.RefreshMarketsJob
import one.mixin.android.job.UpdateFavoriteJob
import one.mixin.android.session.Session
import one.mixin.android.ui.common.Web3Fragment
import one.mixin.android.ui.home.web3.market.TopMenuAdapter
import one.mixin.android.ui.home.web3.market.TopMenuData
import one.mixin.android.ui.home.web3.market.Web3MarketAdapter
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.wallet.WalletActivity.Destination
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.market.GlobalMarket
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class MarketFragment : Web3Fragment(R.layout.fragment_market) {
    companion object {
        const val TAG = "MarketFragment"
        private const val TYPE_ALL = 0
        private const val TYPE_FOV = 1
    }

    private val binding by viewBinding(FragmentMarketBinding::bind)

    @Inject
    lateinit var jobManager: MixinJobManager

    private val walletViewModel by viewModels<WalletViewModel>()
    private val horizontalPadding by lazy { requireContext().screenWidth() / 20 }

    @SuppressLint("SetTextI18n", "DefaultLocale", "NotifyDataSetChanged")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            watchlist.adapter = watchlistAdapter
            markets.adapter = marketsAdapter
            watchlist.itemAnimator = null
            markets.itemAnimator = null
            if (type == TYPE_ALL) {
                radioAll.isChecked = true
                markets.isVisible = true
                watchlist.isVisible = false
                binding.dropSort.isVisible = true
            } else {
                radioFavorites.isChecked = true
                markets.isVisible = false
                watchlist.isVisible = true
                binding.dropSort.isVisible = false
            }
            val itemDecoration = object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State,
                ) {
                    super.getItemOffsets(outRect, view, parent, state)
                    outRect.bottom = 8.dp
                }
            }
            markets.addItemDecoration(itemDecoration)
            watchlist.addItemDecoration(itemDecoration)
            radioGroupMarket.setOnCheckedChangeListener { _, id ->
                type = if (id == R.id.radio_favorites) {
                    TYPE_FOV
                } else {
                    TYPE_ALL
                }
            }
            if (isAsc) {
                icon.rotation = 0f
            } else {
                icon.rotation = 180f
            }
            titleOrder.setOnClickListener {
                if (isAsc) {
                    icon.rotation = 180f
                    isAsc = false
                } else {
                    icon.rotation = 0f
                    isAsc = true
                }
            }
            pirce.updateLayoutParams<MarginLayoutParams> {
                marginEnd = horizontalPadding * 2 + 60.dp
            }
            // Todo switch percentage
            percentage.text = getString(R.string.change_percent_period_day, 7)
            percentage.updateLayoutParams<MarginLayoutParams> {
                marginEnd = horizontalPadding
            }
            titleOrder.updateLayoutParams<MarginLayoutParams> {
                marginStart = horizontalPadding
            }
            root.doOnPreDraw {
                empty.updateLayoutParams<MarginLayoutParams> {
                    topMargin = appBarLayout.height
                }
            }

            dropSort.setOnClickListener {
                binding.sortArrow.animate().rotation(-180f).setDuration(200).start()
                menuAdapter.checkPosition = top
                menuAdapter.notifyDataSetChanged()
                onMenuShow()
                sortMenu.show()
            }
        }
        updateUI()
        loadGlobalMarket()
        RxBus.listen(GlobalMarketEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(destroyScope)
            .subscribe { _ ->
                loadGlobalMarket()
            }
        bindData()
        view.viewTreeObserver.addOnGlobalLayoutListener {
            if (view.isShown) {
                if (job?.isActive == true) return@addOnGlobalLayoutListener
                job = lifecycleScope.launch {
                    delay(60000)
                    updateUI()
                }
            } else {
                job?.cancel()
            }
        }
    }

    private fun loadGlobalMarket() {
        try {
            defaultSharedPreferences.getString(PREF_GLOBAL_MARKET, null)?.let { json ->
                GsonHelper.customGson.fromJson(json, GlobalMarket::class.java)?.let {
                    binding.apply {
                        marketCap.render(R.string.Global_Market_Cap, it.marketCap, BigDecimal(it.marketCapChangePercentage))
                        volume.render(R.string.volume_24h, it.volume, BigDecimal(it.volumeChangePercentage))
                        dominance.render(R.string.Dominance, BigDecimal(it.dominancePercentage), it.dominance)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private var type = MixinApplication.appContext.defaultSharedPreferences.getInt(Constants.Account.PREF_MARKET_TYPE, TYPE_ALL)
        set(value) {
            if (field != value) {
                field = value
                defaultSharedPreferences.putInt(Constants.Account.PREF_MARKET_TYPE, value)
                when (type) {
                    TYPE_ALL -> {
                        binding.dropSort.isVisible = true
                        binding.title.setText(R.string.Market_Cap)
                        binding.markets.isVisible = true
                        binding.watchlist.isVisible = false
                        binding.titleLayout.isVisible = true
                        binding.empty.isVisible = false
                    }

                    else -> {
                        binding.dropSort.isVisible = false
                        binding.title.setText(R.string.Watchlist)
                        binding.markets.isVisible = false
                        if (watchlistAdapter.itemCount == 0) {
                            binding.titleLayout.isVisible = false
                            binding.empty.isVisible = true
                            binding.watchlist.isVisible = false
                        } else {
                            binding.titleLayout.isVisible = true
                            binding.empty.isVisible = false
                            binding.watchlist.isVisible = true
                        }
                    }
                }
            }
        }

    private var top = 0 // 0 is top100, 1 is top200, 2 is top500
        set(value) {
            if (field != value) {
                field = value
                bindData()
            }
        }

    private var isAsc = MixinApplication.appContext.defaultSharedPreferences.getBoolean(Constants.Account.PREF_MARKET_ASC, true)
        set(value) {
            if (field != value) {
                field = value
                bindData()
                MixinApplication.appContext.defaultSharedPreferences.putBoolean(Constants.Account.PREF_MARKET_ASC, value)
            }
        }

    private var lastFiatCurrency: String? = null

    @SuppressLint("NotifyDataSetChanged")
    private fun bindData() {
        val limit = when (top) {
            1 -> 200
            2 -> 500
            else -> 100
        }

        binding.dropTv.text = getString(
            R.string.top_count,
            when (top) {
                1 -> 200
                2 -> 500
                else -> 100
            }
        )
        viewLifecycleOwner.lifecycleScope.launch {
            walletViewModel.getWeb3Markets(limit, isAsc).collectLatest { pagingData ->
                marketsAdapter.submitData(pagingData)
                if (lastFiatCurrency != Session.getFiatCurrency()) {
                    lastFiatCurrency = Session.getFiatCurrency()
                    marketsAdapter.notifyDataSetChanged()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            walletViewModel.getFavoredWeb3Markets(isAsc).collectLatest { pagingData ->
                watchlistAdapter.submitData(pagingData)
                if (lastFiatCurrency != Session.getFiatCurrency()) {
                    lastFiatCurrency = Session.getFiatCurrency()
                    watchlistAdapter.notifyDataSetChanged()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            watchlistAdapter.loadStateFlow.collectLatest { _ ->
                val isEmpty = watchlistAdapter.itemCount == 0
                if (isEmpty && type == TYPE_FOV) {
                    binding.titleLayout.isVisible = false
                    binding.empty.isVisible = true
                    binding.watchlist.isVisible = false
                } else if (type == TYPE_FOV) {
                    binding.titleLayout.isVisible = true
                    binding.empty.isVisible = false
                    binding.watchlist.isVisible = true
                }
            }
        }
    }

    override fun updateUI() {
        jobManager.addJobInBackground(RefreshMarketsJob())
        jobManager.addJobInBackground(RefreshGlobalWeb3MarketJob())
        jobManager.addJobInBackground(RefreshMarketsJob("favorite"))
    }

    private var job: Job? = null

    private val watchlistAdapter by lazy {
        Web3MarketAdapter(true, { marketItem ->
            lifecycleScope.launch {
                val tokens = walletViewModel.findTokensByCoinId(marketItem.coinId)
                if (tokens.size == 1) {
                    WalletActivity.showWithToken(requireActivity(), tokens.first(), Destination.Market, true)
                } else {
                    WalletActivity.showWithMarket(requireActivity(), marketItem, Destination.Market)
                }
            }
        }, { coinId, isFavored ->
            jobManager.addJobInBackground(UpdateFavoriteJob(coinId, isFavored))
        })
    }

    private val marketsAdapter by lazy {
        Web3MarketAdapter(false, { marketItem ->
            lifecycleScope.launch {
                val tokens = walletViewModel.findTokensByCoinId(marketItem.coinId)
                if (tokens.size == 1) {
                    WalletActivity.showWithToken(requireActivity(), tokens.first(), Destination.Market, true)
                } else {
                    WalletActivity.showWithMarket(requireActivity(), marketItem, Destination.Market)
                }
            }
        }, { coinId, isFavored ->
            jobManager.addJobInBackground(UpdateFavoriteJob(coinId, isFavored))
        })
    }

    private val onMenuShow = {
        binding.dropSort.setBackgroundResource(R.drawable.bg_market_drop)
        binding.dropTv.setTextColor(0xFF4B7CDD.toInt())
    }

    private val sortMenu by lazy {
        ListPopupWindow(requireContext()).apply {
            anchorView = binding.dropSort
            setAdapter(menuAdapter)
            setOnItemClickListener { _, _, position, _ ->
                top = position
                dismiss()
            }
            width = 130.dp
            height = ListPopupWindow.WRAP_CONTENT
            isModal = true
            setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_round_white_8dp))
            setDropDownGravity(Gravity.END)
            horizontalOffset = requireContext().dpToPx(2f)
            verticalOffset = requireContext().dpToPx(10f)
            setOnDismissListener {
                onMenuDismiss()
                binding.sortArrow.animate().rotation(0f).setDuration(200).start()
            }
        }
    }

    private val onMenuDismiss = {
        binding.dropSort.setBackgroundResource(R.drawable.bg_market_radio)
        binding.dropTv.setTextColor(requireContext().colorAttr(R.attr.text_primary))
    }

    private val menuAdapter: TopMenuAdapter by lazy {
        val menuItems = listOf(
            TopMenuData(100),
            TopMenuData(200),
            TopMenuData(500),
        )
        TopMenuAdapter(requireContext(), menuItems)
    }
}