package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentMarketBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.textColorResource
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshPriceInfoJob
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.market.Market
import one.mixin.android.ui.wallet.AllTransactionsFragment.Companion.ARGS_TOKEN
import one.mixin.android.util.getChainName
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal
import javax.inject.Inject

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

    private val typeState = mutableStateOf("1D")

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
            balanceRl.setOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
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
                    marketVolC.text = getString(R.string.N_A)
                    marketVolU.text = getString(R.string.N_A)
                    marketCap.text = getString(R.string.N_A)
                    circulationSupply.text = "${info.circulatingSupply} ${asset.symbol}"
                    totalSupply.text = "${info.totalSupply} ${asset.symbol}"
                    issueDate.text = getString(R.string.N_A)
                    issuePrice.text = getString(R.string.N_A)

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