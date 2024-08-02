package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentMarketBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.textColorResource
import one.mixin.android.job.CheckBalanceJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.market.LineChart
import one.mixin.android.ui.wallet.AllTransactionsFragment.Companion.ARGS_TOKEN
import one.mixin.android.util.getChainName
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.assetIdToAsset
import one.mixin.android.vo.market.Price
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class MarketFragment : BaseFragment(R.layout.fragment_market) {
    companion object {
        const val TAG = "MarketFragment"
    }

    private val binding by viewBinding(FragmentMarketBinding::bind)

    @Inject
    lateinit var jobManager: MixinJobManager

    private val walletViewModel by viewModels<WalletViewModel>()

    lateinit var asset: TokenItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        asset = requireArguments().getParcelableCompat(ARGS_TOKEN, TokenItem::class.java)!!
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val changeUsd = BigDecimal(asset.changeUsd)
        val isPositive = changeUsd > BigDecimal.ZERO
        jobManager.addJobInBackground(CheckBalanceJob(arrayListOf(assetIdToAsset(asset.assetId))))
        binding.titleView.apply {
            val sub = getChainName(asset.chainId, asset.chainName, asset.assetKey)
            if (sub != null)
                setSubTitle(asset.name, sub)
            else
                titleTv.text = asset.name
            leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        }
        binding.apply {
            icon.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
            icon.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                val type =
                    when (checkedId) {
                        R.id.radio_1h -> {
                            "1H"
                        }

                        R.id.radio_1d -> {
                            "1D"
                        }

                        R.id.radio_1w -> {
                            "1W"
                        }

                        R.id.radio_1m -> {
                            "1M"
                        }

                        R.id.radio_1y -> {
                            "YTD"
                        }

                        else -> {
                            "ALL"
                        }
                    }
                lifecycleScope.launch {
                    loadPriceHistory(type)?.let { loadPriceHistory(it,isPositive)}
                }
            }


            balance.text = asset.balance
            value.text = try {
                if (asset.fiat().toFloat() == 0f) {
                    "≈ ${Fiats.getSymbol()}0.00"
                } else {
                    "≈ ${Fiats.getSymbol()}${asset.fiat().numberFormat2()}"
                }
            } catch (ignored: NumberFormatException) {
                "≈ ${Fiats.getSymbol()}${asset.fiat().numberFormat2()}"
            }
            if (asset.priceUsd == "0") {
                rise.visibility = GONE
            } else {
                rise.visibility = VISIBLE
                if (asset.changeUsd.isNotEmpty()) {
                    val changeUsd = BigDecimal(asset.changeUsd)
                    val isPositive = changeUsd > BigDecimal.ZERO
                    rise.text = "${(changeUsd * BigDecimal(100)).numberFormat2()}%"
                    priceRise.text = "${(changeUsd * BigDecimal(100)).numberFormat2()}%"
                    rise.textColorResource = if (isPositive) R.color.wallet_green else R.color.wallet_pink
                    priceRise.textColorResource = if (isPositive) R.color.wallet_green else R.color.wallet_pink
                }
            }

            name.text = asset.name
            symbol.text = asset.symbol
            chain.text = asset.chainName

            // Todo real data
            introduction.text = "Ethereum was created in 2015 by Vitalik Buterin, a Russian-Canadian programmer. The platform is based on the principle of decentralization, which means that it is not controlled by any single entity"
            address.text = asset.assetKey
        }
        lifecycleScope.launch {
            walletViewModel.price(asset.assetId).data?.let {
                binding.apply {
                    priceValue.text = "\$${it.currentPrice.numberFormat2()}"
                    marketHigh.text = "\$${it.high24h}"
                    marketLow.text = "\$${it.low24h}"
                    marketVolC.text = "\$3,196.59"
                    marketVolU.text = "2.47B"
                    marketCap.text = "$343.75B"
                    circulationSupply.text = "${it.circulatingSupply} ${asset.symbol}"
                    totalSupply.text = "${it.totalSupply} ${asset.symbol}"
                    issueDate.text = "2024-07-24"
                    issuePrice.text = "$0.308"

                    highValue.text = it.ath
                    highTime.text = it.athDate
                    lowValue.text = it.atl
                    lowTime.text = it.atlDate
                }
            }
        }

        lifecycleScope.launch {
            loadPriceHistory("1D")?.let { loadPriceHistory(it,isPositive)}
        }
    }
    private fun loadPriceHistory(list:List<Float>, isPositive:Boolean){
        binding.market.setContent {
            LineChart(list, isPositive, true)
        }
    }

    private suspend fun loadPriceHistory(type:String): List<Float>? {
        val response = walletViewModel.priceHistory(asset.assetId, type)
        return response.data?.map { it.price.toFloat() }
    }
}