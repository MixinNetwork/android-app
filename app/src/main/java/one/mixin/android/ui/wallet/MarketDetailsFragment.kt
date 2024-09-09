package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.View.VISIBLE
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.drawToBitmap
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDetailsMarketBinding
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.marketPriceFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.extension.priceFormat2
import one.mixin.android.extension.textColorResource
import one.mixin.android.extension.toast
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshMarketJob
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.market.Market
import one.mixin.android.ui.home.web3.market.ChooseTokensBottomSheetDialogFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.market.MarketItem
import one.mixin.android.vo.safe.TokenItem
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class MarketDetailsFragment : BaseFragment(R.layout.fragment_details_market) {
    companion object {
        const val TAG = "MarketDetailsFragment"
        const val ARGS_MARKET = "args_market"
        const val ARGS_ASSET_ID = "args_asset_id"
    }

    private val binding by viewBinding(FragmentDetailsMarketBinding::bind)

    @Inject
    lateinit var jobManager: MixinJobManager

    private val walletViewModel by viewModels<WalletViewModel>()

    private val marketItem: MarketItem by lazy {
        requireNotNull(requireArguments().getParcelableCompat(ARGS_MARKET, MarketItem::class.java))
    }

    private val assetId by lazy {
        requireArguments().getString(ARGS_ASSET_ID)
    }

    private val typeState = mutableStateOf("1D")

    @SuppressLint("SetTextI18n", "DefaultLocale")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val changeUsd = BigDecimal(marketItem.priceChange24h)
        val isPositive = changeUsd > BigDecimal.ZERO
        jobManager.addJobInBackground(RefreshMarketJob(marketItem.coinId))
        binding.apply {
            titleView.apply {
                setSubTitle(marketItem.symbol, marketItem.name)
                leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
                rightExtraIb.isVisible = true
                rightExtraIb.setImageResource(if (marketItem.isFavored == true) R.drawable.ic_title_favorites_checked else R.drawable.ic_title_favorites)
                rightExtraIb.setOnClickListener {
                    walletViewModel.updateMarketFavored(marketItem.symbol, marketItem.coinId, marketItem.isFavored)
                    marketItem.isFavored = !(marketItem.isFavored ?: false)
                    rightExtraIb.setImageResource(if (marketItem.isFavored == true) R.drawable.ic_title_favorites_checked else R.drawable.ic_title_favorites)
                }
                rightIb.setOnClickListener {
                    if (!isLoading) MarketShareActivity.show(requireContext(), marketLl.drawToBitmap(),  marketItem.symbol )
                    else toast(R.string.Please_wait_a_bit)
                }
            }
            nameTitle.text = getString(R.string.Name).uppercase()
            symbolTitle.text = getString(R.string.Symbol).uppercase()
            marketCapTitle.text = getString(R.string.Market_Cap).uppercase()
            circulationSupplyTitle.text = getString(R.string.Circulation_Supply).uppercase()
            totalSupplyTitle.text = getString(R.string.Total_Supply).uppercase()
            allTimeLowTitle.text = getString(R.string.All_Time_Low).uppercase()
            allTimeHighTitle.text = getString(R.string.All_Time_High).uppercase()
            marketCapStatsTitle.text = getString(R.string.Market_Cap).uppercase()
            marketVolUTitle.text = getString(R.string.vol_24h).uppercase()
            riseTitle.text = getString(R.string.hours_count_short, 24)
            radio1d.text = getString(R.string.days_count_short, 1)
            radio1w.text = getString(R.string.weeks_count_short, 1)
            radio1m.text = getString(R.string.months_count_short, 1)
            radioYtd.text = getString(R.string.ytd)
            radioAll.text = getString(R.string.All)
            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                requireActivity().heavyClickVibrate()
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

            name.text = marketItem.name
            symbol.text = marketItem.symbol
            icon.loadToken(marketItem)
            lifecycleScope.launch(CoroutineExceptionHandler { _, error ->
                Timber.e(error)
                balanceRl.isVisible = false
            }) {
                val ids = walletViewModel.findTokenIdsByCoinId(marketItem.coinId)
                val tokens = walletViewModel.findTokensByCoinId(marketItem.coinId)
                if (ids.isNotEmpty()) {
                    val balances = tokens.sumOf { BigDecimal(it.balance) }
                    val price = BigDecimal(marketItem.currentPrice).multiply(BigDecimal(Fiats.getRate())).multiply(balances)
                    balance.text = "${balances.numberFormat8()} ${marketItem.symbol}"
                    value.text = try {
                        if (price == BigDecimal.ZERO) {
                            "≈ ${Fiats.getSymbol()}0.00"
                        } else {
                            "≈ ${Fiats.getSymbol()}${price.numberFormat2()}"
                        }
                    } catch (ignored: NumberFormatException) {
                        "≈ ${Fiats.getSymbol()}${price.numberFormat2()}"
                    }
                    priceRise.visibility = VISIBLE
                    if (balances != BigDecimal.ZERO && marketItem.priceChangePercentage24H.isNotEmpty()) {
                        val change = changeUsd.multiply(balances).multiply(BigDecimal(Fiats.getRate()))
                        currentRise = "${(BigDecimal(marketItem.priceChangePercentage24H)).numberFormat2()}%"
                        priceRise.text = currentRise
                        balanceChange.text = "${if (change >= BigDecimal.ZERO) "+" else "-"}${Fiats.getSymbol()}${change.priceFormat2().replace("-", "")} ($currentRise)"
                        balanceChange.textColorResource = if (isPositive) R.color.wallet_green else R.color.wallet_pink
                        priceRise.textColorResource = if (isPositive) R.color.wallet_green else R.color.wallet_pink
                        riseTitle.isVisible = true
                    } else {
                        balanceChange.setTextColor(requireContext().colorAttr(R.attr.text_assist))
                        priceRise.setTextColor(requireContext().colorAttr(R.attr.text_assist))
                        balanceChange.text = "0.00%"
                        priceRise.text = "0.00%"
                        riseTitle.isVisible = false
                    }
                    balanceRl.setOnClickListener {
                        lifecycleScope.launch {
                            if (ids.size > tokens.size) {
                                val dialog =
                                    indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                                        setCancelable(false)
                                    }
                                dialog.show()
                                walletViewModel.syncNoExistAsset(ids.subtract(tokens.map { it.assetId }.toSet()).toList())
                                dialog.dismiss()
                            }
                            val nowTokens = walletViewModel.findTokensByCoinId(marketItem.coinId)
                            if (nowTokens.isEmpty()) {
                                toast(R.string.Data_error)
                                return@launch
                            }
                            if (nowTokens.size == 1) {
                                if (assetId == nowTokens.first().assetId) {
                                    requireActivity().onBackPressedDispatcher.onBackPressed()
                                } else {
                                    WalletActivity.showWithToken(requireActivity(), nowTokens.first(), WalletActivity.Destination.Transactions, true)
                                }
                            } else {
                                ChooseTokensBottomSheetDialogFragment.newInstance(ArrayList<TokenItem>().apply { addAll(nowTokens) })
                                    .apply {
                                        callback = { token ->
                                            if (assetId == token.assetId) {
                                                activity?.onBackPressedDispatcher?.onBackPressed()
                                            } else {
                                                activity?.let { WalletActivity.showWithToken(it, token, WalletActivity.Destination.Transactions, true) }
                                            }
                                        }
                                    }
                                    .show(parentFragmentManager, ChooseTokensBottomSheetDialogFragment.TAG)
                            }
                        }
                    }
                    balanceRl.isVisible = true
                } else {
                    balanceRl.isVisible = false
                }
            }

            market.setContent {
                Market(typeState.value, marketItem.coinId, { percentageChange ->
                    if (percentageChange == null) {
                        currentRise = percentageChange
                        priceRise.setTextColor(requireContext().colorAttr(R.attr.text_assist))
                        priceRise.text = getString(R.string.N_A)
                    } else {
                        priceRise.textColorResource = if (percentageChange >= 0f) R.color.wallet_green else R.color.wallet_pink
                        currentRise = String.format("%.2f%%", percentageChange)
                        priceRise.text = currentRise
                    }
                }, { price, percentageChange ->
                    if (price == null) {
                        priceRise.text = currentRise
                        priceValue.text = currentPrice
                        priceRise.textColorResource = if (currentRise?.startsWith("-") == true) R.color.wallet_pink else R.color.wallet_green
                    } else {
                        priceValue.text = "${Fiats.getSymbol()}${BigDecimal(price).multiply(BigDecimal(Fiats.getRate())).marketPriceFormat()}"
                        if (percentageChange == null) {
                            priceRise.text = ""
                        } else {
                            priceRise.textColorResource = if (percentageChange >= 0f) R.color.wallet_green else R.color.wallet_pink
                            priceRise.text = String.format("%.2f%%", percentageChange)
                        }
                    }
                }, { loading -> isLoading = loading })
            }
        }

        walletViewModel.marketByCoinId(marketItem.coinId).observe(this.viewLifecycleOwner) { info ->
            if (info != null) {
                binding.apply {
                    currentPrice = priceFormat(info.currentPrice)
                    priceValue.text = currentPrice
                    marketHigh.text = priceFormat(info.high24h)
                    marketLow.text = priceFormat(info.low24h)
                    marketVolU.text = capFormat(info.totalVolume, BigDecimal(Fiats.getRate()), Fiats.getSymbol())

                    if (info.circulatingSupply == "0") {
                        circulationSupply.text = getString(R.string.N_A)
                        circulationSupply.setTextColor(textAssist)
                    } else {
                        circulationSupply.setTextColor(textPrimary)
                        circulationSupply.text = "${info.circulatingSupply.numberFormat8()} ${marketItem.symbol}"
                    }

                    if (info.marketCap == "0" || info.marketCap.isBlank()) {
                        marketCap.text = getString(R.string.N_A)
                        marketCapStats.text = getString(R.string.N_A)
                        marketCap.setTextColor(textAssist)
                        marketCapStats.setTextColor(textAssist)
                    } else {
                        marketCap.setTextColor(textPrimary)
                        marketCapStats.setTextColor(textPrimary)
                        marketCap.text = capFormat(info.marketCap, BigDecimal(Fiats.getRate()), Fiats.getSymbol())
                        marketCapStats.text = capFormat(info.marketCap, BigDecimal(Fiats.getRate()), Fiats.getSymbol())
                    }
                    totalSupply.text = "${info.totalSupply.numberFormat8()} ${marketItem.symbol}"

                    highValue.text = priceFormat(info.ath)
                    highTime.isVisible = true
                    highTime.text = info.athDate
                    lowValue.text = priceFormat(info.atl)
                    lowTime.isVisible = true
                    lowTime.text = info.atlDate

                    priceValue.setTextColor(textPrimary)
                    marketCap.setTextColor(textPrimary)
                    marketHigh.setTextColor(textPrimary)
                    marketLow.setTextColor(textPrimary)
                    marketCapStats.setTextColor(textPrimary)
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
                    marketCapStats.setTextColor(textAssist)
                    marketCapStats.setText(R.string.N_A)
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

    private var isLoading = false

    private val textAssist by lazy {
        requireContext().colorAttr(R.attr.text_assist)
    }

    private val textPrimary by lazy {
        requireContext().colorAttr(R.attr.text_primary)
    }

    private fun volFormat(vol: String, rate: BigDecimal, symbol: String? = null): String {
        val formatVol = try {
            BigDecimal(vol).multiply(rate).numberFormat2()
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

    private fun capFormat(vol: String, rate: BigDecimal, symbol: String): String {
        val formatVol = try {
            BigDecimal(vol).multiply(rate).numberFormatCompact()
        } catch (e: NumberFormatException) {
            null
        }
        if (formatVol != null) {
            return "$symbol$formatVol"
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

    private var currentPrice: String? = null
    private var currentRise: String? = null
}