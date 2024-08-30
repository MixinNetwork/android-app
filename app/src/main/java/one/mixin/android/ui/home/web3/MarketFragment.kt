package one.mixin.android.ui.home.web3

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
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
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.putInt
import one.mixin.android.extension.screenWidth
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshMarketsJob
import one.mixin.android.job.UpdateFavoriteJob
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.market.MarketItem
import one.mixin.android.extension.dp
import one.mixin.android.job.RefreshGlobalWeb3MarketJob
import one.mixin.android.ui.common.Web3Fragment
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
            } else {
                radioFavorites.isChecked = true
                markets.isVisible = false
                watchlist.isVisible = true
            }
            radioGroupMarket.setOnCheckedChangeListener { _, id ->
                type = if (id == R.id.radio_favorites) {
                    TYPE_FOV
                } else {
                    TYPE_ALL
                }
            }
            percentage.updateLayoutParams<MarginLayoutParams> {
                marginEnd = horizontalPadding
            }
            root.doOnPreDraw {
                empty.updateLayoutParams<MarginLayoutParams> {
                    topMargin = appBarLayout.height
                }
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
                        binding.title.setText(R.string.Market_Cap)
                        binding.markets.isVisible = true
                        binding.watchlist.isVisible = false
                        binding.titleLayout.isVisible = true
                        binding.empty.isVisible = false
                    }

                    else -> {
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

    @SuppressLint("NotifyDataSetChanged")
    private fun bindData() {
        walletViewModel.getWeb3Markets().observe(this.viewLifecycleOwner) { list ->
            marketsAdapter.items = list
        }

        walletViewModel.getFavoredWeb3Markets().observe(this.viewLifecycleOwner) { list ->
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
        Web3MarketAdapter({ marketItem ->
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
        Web3MarketAdapter({ marketItem ->
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

    class Web3MarketAdapter(private val onClick: (MarketItem) -> Unit, private val onFavorite: (String, Boolean?) -> Unit) : RecyclerView.Adapter<Web3MarketAdapter.ViewHolder>() {
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
            fun bind(item: MarketItem, onClick: (MarketItem) -> Unit, onFavorite: (String, Boolean?) -> Unit) {
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
                    assetNumber.text = "${absoluteAdapterPosition + 1}"
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
            holder.bind(items[position], onClick, onFavorite)
        }

        override fun getItemCount(): Int = items.size
    }
}