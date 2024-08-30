package one.mixin.android.ui.home.web3

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.PREF_GLOBAL_MARKET
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.FragmentMarketBinding
import one.mixin.android.databinding.ItemMarketBinding
import one.mixin.android.event.GlobalMarketEvent
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.putInt
import one.mixin.android.extension.screenWidth
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshMarketsJob
import one.mixin.android.job.UpdateFavoriteJob
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.market.MarketItem
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.job.RefreshGlobalWeb3MarketJob
import one.mixin.android.tip.wc.SortOrder
import one.mixin.android.ui.common.Web3Fragment
import one.mixin.android.ui.home.inscription.menu.SortMenuAdapter
import one.mixin.android.ui.home.inscription.menu.SortMenuData
import one.mixin.android.ui.home.web3.market.TopMenuAdapter
import one.mixin.android.ui.home.web3.market.TopMenuData
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.wallet.WalletActivity.Destination
import one.mixin.android.util.GsonHelper
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
            radioGroupMarket.setOnCheckedChangeListener { _, id ->
                type = if (id == R.id.radio_favorites) {
                    TYPE_FOV
                } else {
                    TYPE_ALL
                }
            }
            pirce.updateLayoutParams<MarginLayoutParams> {
                marginEnd = horizontalPadding * 2 + 60.dp
            }
            percentage.updateLayoutParams<MarginLayoutParams> {
                marginEnd = horizontalPadding
            }
            title.updateLayoutParams<MarginLayoutParams> {
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
                        marketCap.render(R.string.Market_Cap, it.marketCap, BigDecimal(it.marketCapChangePercentage))
                        volume.render(R.string.Volume, it.volume, BigDecimal(it.volumeChangePercentage))
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

    @SuppressLint("NotifyDataSetChanged")
    private fun bindData() {
        val limit = when (top) {
            1 -> 200
            2 -> 500
            else -> 100
        }
        binding.dropTv.setText(
            when (top) {
                1 -> R.string.top_200
                2 -> R.string.top_500
                else -> R.string.top_100
            }
        )
        walletViewModel.getWeb3Markets(limit).observe(this.viewLifecycleOwner) { list ->
            marketsAdapter.items = list
        }
        walletViewModel.getFavoredWeb3Markets(limit).observe(this.viewLifecycleOwner) { list ->
            if (list.isEmpty() && type == TYPE_FOV) {
                binding.titleLayout.isVisible = false
                binding.empty.isVisible = true
                binding.watchlist.isVisible = false
            } else if (type == TYPE_FOV) {
                binding.titleLayout.isVisible = true
                binding.empty.isVisible = false
                binding.watchlist.isVisible = true
            }
            watchlistAdapter.items = list
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
                val token = walletViewModel.findTokenByCoinId(marketItem.coinId)
                if (token != null) {
                    WalletActivity.showWithToken(requireActivity(), token, Destination.Market)
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
                val token = walletViewModel.findTokenByCoinId(marketItem.coinId)
                if (token != null) {
                    WalletActivity.showWithToken(requireActivity(), token, Destination.Market)
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
            width = ListPopupWindow.WRAP_CONTENT
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
            TopMenuData(100, R.string.top_100),
            TopMenuData(200, R.string.top_200),
            TopMenuData(500, R.string.top_500),
        )
        TopMenuAdapter(requireContext(), menuItems)
    }

    class MarketDiffCallback(
        private val oldList: List<MarketItem>,
        private val newList: List<MarketItem>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].coinId == newList[newItemPosition].coinId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    class Web3MarketAdapter(private val sourceRank: Boolean, private val onClick: (MarketItem) -> Unit, private val onFavorite: (String, Boolean?) -> Unit) : RecyclerView.Adapter<Web3MarketAdapter.ViewHolder>() {
        var items: List<MarketItem> = emptyList()
            set(value) {
                val diffResult = DiffUtil.calculateDiff(MarketDiffCallback(field, value))
                field = value
                diffResult.dispatchUpdatesTo(this)
            }

        class ViewHolder(val binding: ItemMarketBinding) : RecyclerView.ViewHolder(binding.root) {
            private val horizontalPadding by lazy { binding.root.context.screenWidth() / 20 }
            private val verticalPadding by lazy { 6.dp }

            init {
                binding.container.setPadding(horizontalPadding - 4.dp, verticalPadding, horizontalPadding, verticalPadding)
                binding.price.updateLayoutParams<MarginLayoutParams> {
                    marginEnd = horizontalPadding
                }
            }

            @SuppressLint("CheckResult", "SetTextI18n")
            fun bind(item: MarketItem, sourceRank: Boolean, onClick: (MarketItem) -> Unit, onFavorite: (String, Boolean?) -> Unit) {
                binding.apply {
                    root.setOnClickListener { onClick.invoke(item) }
                    val symbol = Fiats.getSymbol()
                    val rate = BigDecimal(Fiats.getRate())
                    favorite.setImageResource(if (item.isFavored == true) R.drawable.ic_market_favorites_checked else R.drawable.ic_market_favorites)
                    favorite.setOnClickListener {
                        onFavorite.invoke(item.coinId, item.isFavored)
                    }
                    icon.loadImage(item.iconUrl, R.drawable.ic_avatar_place_holder)
                    assetSymbol.text = item.symbol
                    assetValue.text = item.totalVolume
                    price.text = "$symbol${BigDecimal(item.currentPrice).multiply(rate).priceFormat()}"
                    assetNumber.text = if (sourceRank) item.marketCapRank else "${absoluteAdapterPosition + 1}"
                    val formatVol = try {
                        BigDecimal(item.totalVolume).multiply(rate).numberFormatCompact()
                    } catch (e: NumberFormatException) {
                        null
                    }
                    assetValue.text = if (formatVol != null) {
                        "$symbol$formatVol"
                    } else {
                        ""
                    }
                    market.loadImage(item.sparklineIn7d)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemMarketBinding.inflate(LayoutInflater.from(parent.context)))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position], sourceRank, onClick, onFavorite)
        }

        override fun getItemCount(): Int = items.size
    }
}