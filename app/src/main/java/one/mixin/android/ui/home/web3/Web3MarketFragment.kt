package one.mixin.android.ui.home.web3

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.FragmentWeb3MarketBinding
import one.mixin.android.databinding.ItemWeb3MarketBinding
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
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class Web3MarketFragment : BaseFragment(R.layout.fragment_web3_market) {
    companion object {
        const val TAG = "MarketFragment"
        private const val TYPE_ALL = 0
        private const val TYPE_FOV = 1
    }

    private val binding by viewBinding(FragmentWeb3MarketBinding::bind)

    @Inject
    lateinit var jobManager: MixinJobManager

    private val walletViewModel by viewModels<WalletViewModel>()

    @SuppressLint("SetTextI18n", "DefaultLocale", "NotifyDataSetChanged")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            rv.adapter = adapter
            if (type == TYPE_ALL) {
                radioAll.isChecked = true
            } else {
                radioFavorites.isChecked = true
            }
            radioGroupMarket.setOnCheckedChangeListener { _, id ->
                type = if (id == R.id.radio_favorites) {
                    TYPE_FOV
                } else {
                    TYPE_ALL
                }
            }
        }

        jobManager.addJobInBackground(RefreshMarketsJob())
        jobManager.addJobInBackground(RefreshGlobalWeb3MarketJob())
        jobManager.addJobInBackground(RefreshMarketsJob("favorite"))
        walletViewModel.getGlobalWeb3Market().observe(this.viewLifecycleOwner) {
            if (it != null) {
                binding.apply {
                    marketCap.render(R.string.Market_Cap, it.marketCap, BigDecimal(it.marketCapChangePercentage))
                    volume.render(R.string.Volume, it.volume, BigDecimal(it.volumeChangePercentage))
                    dominance.render(R.string.Dominance, BigDecimal(it.dominancePercentage), it.dominance)
                }
            }
        }
        bindData()
    }

    private var type = MixinApplication.appContext.defaultSharedPreferences.getInt(Constants.Account.PREF_MARKET_TYPE, TYPE_ALL)
        set(value) {
            if (field != value) {
                field = value
                defaultSharedPreferences.putInt(Constants.Account.PREF_MARKET_TYPE, value)
                bindData()
            }
        }

    @SuppressLint("NotifyDataSetChanged")
    private fun bindData() {
        when (type) {
            TYPE_ALL -> {
                walletViewModel.getWeb3Markets().observe(this.viewLifecycleOwner) { list ->
                    adapter.items = list
                    adapter.notifyDataSetChanged()
                }
            }

            else -> {
                walletViewModel.getFavoredWeb3Markets().observe(this.viewLifecycleOwner) { list ->
                    adapter.items = list
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private val adapter by lazy {
        Web3MarketAdapter { coinId, isFavored ->
            jobManager.addJobInBackground(UpdateFavoriteJob(coinId, isFavored))
        }
    }

    class Web3MarketAdapter(val onClick: (String, Boolean?) -> Unit) : RecyclerView.Adapter<Web3MarketAdapter.ViewHolder>() {
        var items: List<MarketItem> = emptyList()

        class ViewHolder(val binding: ItemWeb3MarketBinding) : RecyclerView.ViewHolder(binding.root) {
            private val horizontalPadding by lazy { binding.root.context.screenWidth() / 20 }
            private val verticalPadding by lazy { 6.dp }

            init {
                binding.container.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
                binding.price.updateLayoutParams<MarginLayoutParams> {
                    marginEnd = horizontalPadding
                }
            }

            @SuppressLint("CheckResult", "SetTextI18n")
            fun bind(item: MarketItem, onClick: (String, Boolean?) -> Unit) {
                binding.apply {
                    val symbol = Fiats.getSymbol()
                    val rate = BigDecimal(Fiats.getRate())
                    favorite.setImageResource(if (item.isFavored == true) R.drawable.ic_market_favorites_checked else R.drawable.ic_market_favorites)
                    favorite.setOnClickListener {
                        onClick.invoke(item.coinId, item.isFavored)
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
            return ViewHolder(ItemWeb3MarketBinding.inflate(LayoutInflater.from(parent.context)))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position], onClick)
        }

        override fun getItemCount(): Int = items.size
    }
}