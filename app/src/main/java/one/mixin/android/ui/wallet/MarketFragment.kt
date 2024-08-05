package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.compose.hiltViewModel
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
import one.mixin.android.job.RefreshPriceInfoJob
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.market.LineChart
import one.mixin.android.ui.home.market.Market
import one.mixin.android.ui.home.web3.Web3ViewModel
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

    val typeState = mutableStateOf("1D")

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val changeUsd = BigDecimal(asset.changeUsd)
        val isPositive = changeUsd > BigDecimal.ZERO
        jobManager.addJobInBackground(RefreshPriceInfoJob(asset.assetId))
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
                typeState.value =
                    when (checkedId) {

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

            market.setContent {
                Market(typeState.value, asset.assetId, isPositive)
            }
        }

        walletViewModel.priceInfo(asset.assetId).observe(this.viewLifecycleOwner) { info->
            if (info != null) {
                binding.apply {
                    priceValue.text = "\$${info.currentPrice}"
                    marketHigh.text = "\$${info.high24h}"
                    marketLow.text = "\$${info.low24h}"
                    marketVolC.text = "\$3,196.59"
                    marketVolU.text = "2.47B"
                    marketCap.text = "$343.75B"
                    circulationSupply.text = "${info.circulatingSupply} ${asset.symbol}"
                    totalSupply.text = "${info.totalSupply} ${asset.symbol}"
                    issueDate.text = "2024-07-24"
                    issuePrice.text = "$0.308"

                    highValue.text = info.ath
                    highTime.isVisible = true
                    highTime.text = info.athDate
                    lowValue.text = info.atl
                    lowTime.isVisible = true
                    lowTime.text = info.atlDate
                }
            } else {
                // Todo
            }
        }
    }
}