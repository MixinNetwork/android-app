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
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.textColorResource
import one.mixin.android.extension.toast
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshMarketJob
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.market.Market
import one.mixin.android.ui.wallet.AllTransactionsFragment.Companion.ARGS_TOKEN
import one.mixin.android.util.getChainName
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.market.Price
import one.mixin.android.vo.safe.TokenItem
import java.math.BigDecimal
import java.math.RoundingMode
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

    @SuppressLint("SetTextI18n", "DefaultLocale")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val changeUsd = BigDecimal(asset.changeUsd)
        val isPositive = changeUsd > BigDecimal.ZERO
        jobManager.addJobInBackground(RefreshMarketJob(asset.assetId))
        binding.apply {
            titleView.apply {
                val sub = getChainName(asset.chainId, asset.chainName, asset.assetKey)
                if (sub != null)
                    setSubTitle(asset.name, sub)
                else
                    titleTv.text = asset.name
                leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
            }
            nameTitle.text = getString(R.string.Name).uppercase()
            symbolTitle.text = getString(R.string.Symbol).uppercase()
            chainTitle.text = getString(R.string.Chain).uppercase()
            contactAddressTitle.text = getString(R.string.Contract_Address).uppercase()
            circulationSupplyTitle.text = getString(R.string.Circulation_Supply).uppercase()
            totalSupply.text = getString(R.string.Total_Supply).uppercase()
            allTimeLowTitle.text = getString(R.string.All_Time_Low).uppercase()
            allTimeHighTitle.text = getString(R.string.All_Time_High).uppercase()
            marketVolCTitle.text = getString(R.string.vol_24h, asset.symbol)
            marketVolUTitle.text = getString(R.string.vol_24h, Fiats.getAccountCurrencyAppearance())
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
                    currentRise = "${(changeUsd * BigDecimal(100)).numberFormat2()}%"
                    rise.text = currentRise
                    priceRise.text = currentRise
                    rise.textColorResource = if (isPositive) R.color.wallet_green else R.color.wallet_pink
                    priceRise.textColorResource = if (isPositive) R.color.wallet_green else R.color.wallet_pink
                }
            }

            name.text = asset.name
            symbol.text = asset.symbol
            chain.text = asset.chainName
            address.text = asset.assetKey
            market.setContent {
                Market(typeState.value, asset.assetId, isPositive) { price, percentageChange ->
                    if (price == null) {
                        priceRise.text = currentRise
                        priceValue.text = currentPrice
                        priceRise.textColorResource = if (isPositive) R.color.wallet_green else R.color.wallet_pink
                    } else {
                        priceValue.text = "$$price"
                        if (percentageChange == null) {
                            priceRise.text = ""
                        } else {
                            priceRise.textColorResource = if (percentageChange >= 0f) R.color.wallet_green else R.color.wallet_pink
                            priceRise.text = String.format("%.2f%%", percentageChange)
                        }
                    }
                }
            }
        }

        walletViewModel.marketById(asset.assetId).observe(this.viewLifecycleOwner) { info->
            if (info != null) {
                binding.apply {
                    currentPrice = priceFormat(info.currentPrice)
                    priceValue.text = currentPrice
                    marketHigh.text = priceFormat(info.high24h)
                    marketLow.text = priceFormat(info.low24h)
                    marketVolC.text = volFormat(info.totalVolume, BigDecimal(info.currentPrice))
                    marketVolU.text = volFormat(info.totalVolume, BigDecimal(Fiats.getRate()), Fiats.getSymbol())

                    circulationSupply.text = "${info.circulatingSupply} ${asset.symbol}"
                    totalSupply.text = "${info.totalSupply} ${asset.symbol}"

                    highValue.text = info.ath
                    highTime.isVisible = true
                    highTime.text = info.athDate
                    lowValue.text = info.atl
                    lowTime.isVisible = true
                    lowTime.text = info.atlDate
                }
            }
        }
    }

    private fun volFormat(vol: String, rate: BigDecimal, symbol: String? = null): String {
        val formatVol = try {
            BigDecimal(vol).divide(rate, 2, RoundingMode.HALF_UP).toPlainString()
        } catch (e: NumberFormatException) {
            null
        }
        if (formatVol != null) {
            if (symbol != null) {
                return "$symbol$formatVol"
            }
            return formatVol
        }
        return getString(R.string.N_A)
    }


    private fun priceFormat(price: String): String {
        val formatPrice = try {
            BigDecimal(price).multiply(BigDecimal(Fiats.getRate())).numberFormat8()
        } catch (e: NumberFormatException) {
            null
        }
        if (formatPrice != null) {
            return "${Fiats.getSymbol()} $formatPrice"
        }
        return getString(R.string.N_A)
    }

    private var currentPrice:String? = null
    private var currentRise:String? = null
}