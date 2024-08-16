package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentMarketBinding
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.marketPriceFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.textColorResource
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshMarketJob
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.market.Market
import one.mixin.android.ui.wallet.AllTransactionsFragment.Companion.ARGS_TOKEN
import one.mixin.android.util.getChainName
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats
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
            marketCapTitle.text = getString(R.string.Market_Cap).uppercase()
            circulationSupplyTitle.text = getString(R.string.Circulation_Supply).uppercase()
            totalSupplyTitle.text = getString(R.string.Total_Supply).uppercase()
            allTimeLowTitle.text = getString(R.string.All_Time_Low).uppercase()
            allTimeHighTitle.text = getString(R.string.All_Time_High).uppercase()
            marketVolCTitle.text = getString(R.string.vol_24h).uppercase()
            marketVolUTitle.text = getString(R.string.vol_24h).uppercase()
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

                        R.id.radio_ytd -> {
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
                priceRise.setTextColor(requireContext().colorAttr(R.attr.text_assist))
                priceRise.text = "0.00%"
            } else {
                priceRise.visibility = VISIBLE
                if (asset.changeUsd.isNotEmpty()) {
                    currentRise = "${(changeUsd * BigDecimal(100)).numberFormat2()}%"
                    rise.text = currentRise
                    priceRise.text = currentRise
                    rise.textColorResource = if (isPositive) R.color.wallet_green else R.color.wallet_pink
                    priceRise.textColorResource = if (isPositive) R.color.wallet_green else R.color.wallet_pink
                } else {
                    rise.setTextColor(requireContext().colorAttr(R.attr.text_assist))
                    rise.text = "0.00%"
                }
            }

            name.text = asset.name
            symbol.text = asset.symbol
            chain.text = asset.chainName
            address.text = asset.assetKey
            market.setContent {
                Market(typeState.value, asset.assetId, { percentageChange->
                    if (percentageChange == null) {
                        priceRise.setTextColor(requireContext().colorAttr(R.attr.text_assist))
                        priceRise.text = getString(R.string.N_A)
                    } else {
                        priceRise.textColorResource = if (percentageChange >= 0f) R.color.wallet_green else R.color.wallet_pink
                        priceRise.text = String.format("%.2f%%", percentageChange)
                    }
                },{ price, percentageChange ->
                    if (price == null) {
                        priceRise.text = currentRise
                        priceValue.text = currentPrice
                        priceRise.textColorResource = if (isPositive) R.color.wallet_green else R.color.wallet_pink
                    } else {
                        priceValue.text = "${Fiats.getSymbol()}${BigDecimal(price).multiply(BigDecimal(Fiats.getRate())).marketPriceFormat()}"
                        if (percentageChange == null) {
                            priceRise.text = ""
                        } else {
                            priceRise.textColorResource = if (percentageChange >= 0f) R.color.wallet_green else R.color.wallet_pink
                            priceRise.text = String.format("%.2f%%", percentageChange)
                        }
                    }
                })
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

                    if (info.circulatingSupply == "0") {
                        circulationSupply.text = getString(R.string.N_A)
                        circulationSupply.setTextColor(textAssist)
                    } else {
                        circulationSupply.setTextColor(textPrimary)
                        circulationSupply.text = "${info.circulatingSupply.numberFormat8()} ${asset.symbol}"
                    }

                    if (info.marketCap == "0" || info.marketCap.isBlank()) {
                        marketCap.text = getString(R.string.N_A)
                        marketCap.setTextColor(textAssist)
                    } else {
                        marketCap.setTextColor(textPrimary)
                        marketCap.text = volFormat(info.marketCap, BigDecimal(Fiats.getRate()), Fiats.getSymbol())
                    }
                    totalSupply.text = "${info.totalSupply.numberFormat8()} ${asset.symbol}"

                    highValue.text = info.ath.numberFormat8()
                    highTime.isVisible = true
                    highTime.text = info.athDate
                    lowValue.text = info.atl.numberFormat8()
                    lowTime.isVisible = true
                    lowTime.text = info.atlDate

                    priceValue.setTextColor(textPrimary)
                    marketCap.setTextColor(textPrimary)
                    marketHigh.setTextColor(textPrimary)
                    marketLow.setTextColor(textPrimary)
                    marketVolC.setTextColor(textPrimary)
                    marketVolU.setTextColor(textPrimary)
                    totalSupply.setTextColor(textPrimary)
                    highValue.setTextColor(textPrimary)
                    lowValue.setTextColor(textPrimary)
                    radioGroup.isInvisible = false
                }
            } else {
                binding.apply {
                    radioGroup.isInvisible = true
                    priceValue.setTextColor(textAssist)
                    priceValue.setText(R.string.N_A)
                    marketCap.setTextColor(textAssist)
                    marketCap.setText(R.string.N_A)
                    marketHigh.setTextColor(textAssist)
                    marketHigh.setText(R.string.N_A)
                    marketLow.setTextColor(textAssist)
                    marketLow.setText(R.string.N_A)
                    marketVolC.setTextColor(textAssist)
                    marketVolC.setText(R.string.N_A)
                    marketVolU.setTextColor(textAssist)
                    marketVolU.setText(R.string.N_A)
                    circulationSupply.setTextColor(textAssist)
                    circulationSupply.setText(R.string.N_A)
                    totalSupply.setTextColor(textAssist)
                    totalSupply.setText(R.string.N_A)
                    highValue.setTextColor(textAssist)
                    highValue.setText(R.string.N_A)
                    lowValue.setTextColor(textAssist)
                    lowValue.setText(R.string.N_A)
                }
            }
        }
    }

    private val textAssist by lazy {
        requireContext().colorAttr(R.attr.text_assist)
    }

    private val textPrimary by lazy {
        requireContext().colorAttr(R.attr.text_primary)
    }

    private fun volFormat(vol: String, rate: BigDecimal, symbol: String? = null): String {
        val formatVol = try {
            BigDecimal(vol).divide(rate, 2, RoundingMode.HALF_UP).numberFormat2()
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
            BigDecimal(price).multiply(BigDecimal(Fiats.getRate())).marketPriceFormat()
        } catch (e: NumberFormatException) {
            null
        }
        if (formatPrice != null) {
            return "${Fiats.getSymbol()}$formatPrice"
        }
        return getString(R.string.N_A)
    }

    private var currentPrice:String? = null
    private var currentRise:String? = null
}