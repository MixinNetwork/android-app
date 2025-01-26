package one.mixin.android.ui.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.View.VISIBLE
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.drawToBitmap
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import one.mixin.android.Constants.AssetId.USDT_ASSET_ID
import one.mixin.android.Constants.AssetId.XIN_ASSET_ID
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDetailsMarketBinding
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.marketPriceFormat
import one.mixin.android.extension.navigate
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.extension.priceFormat2
import one.mixin.android.extension.setQuoteText
import one.mixin.android.extension.setQuoteTextWithBackgroud
import one.mixin.android.extension.toast
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshMarketJob
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.market.Market
import one.mixin.android.ui.home.web3.market.ChooseTokensBottomSheetDialogFragment
import one.mixin.android.ui.home.web3.swap.SwapFragment.Companion.ARGS_INPUT
import one.mixin.android.ui.home.web3.swap.SwapFragment.Companion.ARGS_OUTPUT
import one.mixin.android.ui.home.web3.swap.SwapFragment.Companion.ARGS_TOKEN_ITEMS
import one.mixin.android.ui.wallet.alert.AlertFragment.Companion.ARGS_COIN
import one.mixin.android.ui.wallet.alert.AlertFragment.Companion.ARGS_GO_ALERT
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
        jobManager.addJobInBackground(RefreshMarketJob(marketItem.coinId, true))
        binding.apply {
            titleView.apply {
                setSubTitle(marketItem.symbol, marketItem.name)
                leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
                rightExtraIb.isVisible = true
                rightExtraIb.setImageResource(if (marketItem.isFavored == true) R.drawable.ic_title_favorites_checked else R.drawable.ic_title_favorites)
                rightExtraIb.setOnClickListener {
                    walletViewModel.updateMarketFavored(marketItem.symbol, marketItem.coinId, marketItem.isFavored)
                    marketItem.isFavored = marketItem.isFavored != true
                    rightExtraIb.setImageResource(if (marketItem.isFavored == true) R.drawable.ic_title_favorites_checked else R.drawable.ic_title_favorites)
                }
                rightIb.setOnClickListener {
                    if (!isLoading || marketItem.coinId.isBlank()) MarketShareActivity.show(requireContext(), marketLl.drawToBitmap(), marketItem.symbol)
                    else toast(R.string.Please_wait_a_bit)
                }
            }
            swapAlert.swap.setOnClickListener {
                lifecycleScope.launch {
                    val ids = walletViewModel.findTokenIdsByCoinId(marketItem.coinId)
                    val tokens = walletViewModel.findTokensByCoinId(marketItem.coinId)
                    if (ids.size > tokens.size) {
                        val dialog =
                            indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                                setCancelable(false)
                            }
                        dialog.show()
                        walletViewModel.syncNoExistAsset(ids.subtract(tokens.map { it.assetId }.toSet()).toList())
                        dialog.dismiss()
                    }
                    var nowTokens = walletViewModel.findTokensByCoinId(marketItem.coinId)
                    if (nowTokens.isEmpty()) {
                        nowTokens = marketItem.assetIds?.let {
                            walletViewModel.findAssetsByIds(it)
                        } ?: emptyList()
                    }
                    if (nowTokens.isEmpty()) {
                        toast(getString(R.string.swap_not_supported, marketItem.name))
                        return@launch
                    }
                    val assets = walletViewModel.allAssetItems()
                    if (nowTokens.size == 1) {
                        val input = if (nowTokens.first().assetId == USDT_ASSET_ID) {
                            XIN_ASSET_ID
                        } else {
                            USDT_ASSET_ID
                        }

                        view.navigate(R.id.action_market_details_to_swap,
                            Bundle().apply {
                                putString(ARGS_INPUT, input)
                                putString(ARGS_OUTPUT, nowTokens.first().assetId)
                                putParcelableArrayList(ARGS_TOKEN_ITEMS, arrayListOf<TokenItem>().apply { addAll(assets) })
                            })
                    } else {
                        ChooseTokensBottomSheetDialogFragment.newInstance(ArrayList<TokenItem>().apply { addAll(nowTokens) }).apply {
                            callback = { token ->
                                val output = if (token.assetId == USDT_ASSET_ID) {
                                    XIN_ASSET_ID
                                } else {
                                    USDT_ASSET_ID
                                }

                                view.navigate(R.id.action_market_details_to_swap,
                                    Bundle().apply {
                                        putString(ARGS_INPUT, token.assetId)
                                        putString(ARGS_OUTPUT, output)
                                        putParcelableArrayList(ARGS_TOKEN_ITEMS, arrayListOf<TokenItem>().apply { addAll(assets) })
                                    })
                            }
                        }.show(parentFragmentManager, ChooseTokensBottomSheetDialogFragment.TAG)
                    }
                }
            }
            if (marketItem.coinId.isBlank()) {
                walletViewModel.anyAlertByAssetId(marketItem.assetIds!!.first())
            } else {
                walletViewModel.anyAlertByCoinId(marketItem.coinId)
            }.observe(this@MarketDetailsFragment.viewLifecycleOwner) { exist ->
                swapAlert.setAlertTitle(if (exist) R.string.Alert else R.string.Add_Alert)
                swapAlert.alertVa.setOnClickListener {
                    if (NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
                        lifecycleScope.launch {
                            var coinItem = if (marketItem.coinId.isBlank()) {
                                walletViewModel.simpleCoinItemByAssetId(marketItem.assetIds!!.first())
                            } else {
                                walletViewModel.simpleCoinItem(marketItem.coinId)
                            }
                            if (coinItem == null) {
                                binding.swapAlert.alertVa.displayedChild = 1
                                val m = walletViewModel.refreshMarket(
                                    marketItem.coinId.ifBlank {
                                        marketItem.assetIds!!.first()
                                    }, {
                                        binding.swapAlert.alertVa.displayedChild = 0
                                    }, { error ->
                                        if (error.errorCode == 404) {
                                            toast(R.string.Alert_Not_Support)
                                        } else {
                                            toast(R.string.Try_Again)
                                        }
                                        true
                                    }, {
                                        toast(R.string.Try_Again)
                                        true
                                    }
                                )
                                if (m != null) {
                                    coinItem = if (marketItem.coinId.isBlank()) {
                                        walletViewModel.simpleCoinItemByAssetId(marketItem.assetIds!!.first())
                                    } else {
                                        walletViewModel.simpleCoinItem(marketItem.coinId)
                                    }
                                    view.navigate(R.id.action_market_details_to_alert, Bundle().apply {
                                        putParcelable(ARGS_COIN, coinItem)
                                        putBoolean(ARGS_GO_ALERT, !exist)
                                    })
                                }
                            } else {
                                view.navigate(R.id.action_market_details_to_alert, Bundle().apply {
                                    putParcelable(ARGS_COIN, coinItem)
                                    putBoolean(ARGS_GO_ALERT, !exist)
                                })
                            }
                        }
                    } else {
                        toast(getString(R.string.price_alert_notification_permission))
                    }
                }
            }

            if (marketItem.coinId.isBlank()) {
                assetRank.isVisible = false
                titleView.rightExtraIb.isVisible = false
            }
            assetSymbol.text = marketItem.symbol
            assetName.text = marketItem.name
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
            radioAll.text = getString(R.string.All).uppercase()
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
                            "1Y"
                        }

                        else -> {
                            "ALL"
                        }
                    }
            }

            name.text = marketItem.name
            symbol.text = marketItem.symbol
            icon.loadToken(marketItem)

            market.setContent {
                Market(typeState.value, marketItem.coinId.ifBlank {
                    marketItem.assetIds!!.first()
                }, { percentageChange ->
                    if (percentageChange == null) {
                        priceRise.setQuoteTextWithBackgroud(getString(R.string.N_A))
                    } else if (typeState.value == "1D") {
                        val rise = BigDecimal(marketItem.priceChangePercentage24H)
                        currentRise = "${rise.numberFormat2()}%"
                        priceRise.setQuoteTextWithBackgroud(currentRise, rise >= BigDecimal.ZERO)
                    } else {
                        currentRise = String.format("%.2f%%", percentageChange)
                        priceRise.setQuoteTextWithBackgroud(currentRise, percentageChange >= 0f)
                    }
                }, { price, percentageChange ->
                    if (price == null) {
                        priceValue.text = currentPrice
                        priceRise.setQuoteTextWithBackgroud(currentRise, currentRise?.startsWith("-") == false)
                    } else {
                        priceValue.text = "${Fiats.getSymbol()}${BigDecimal(price).multiply(BigDecimal(Fiats.getRate())).marketPriceFormat()}"
                        if (percentageChange == null) {
                            priceRise.setQuoteTextWithBackgroud(getString(R.string.N_A))
                        } else {
                            priceRise.setQuoteTextWithBackgroud(String.format("%.2f%%", percentageChange), percentageChange >= 0f)
                        }
                    }
                }, { loading -> isLoading = loading })
            }
        }
        loadBalance(marketItem)
        if (marketItem.coinId.isBlank()) {
            walletViewModel.marketById(marketItem.assetIds!!.first())
        } else {
            walletViewModel.marketByCoinId(marketItem.coinId)
        }.observe(this.viewLifecycleOwner) { info ->
            if (info != null) {
                loadBalance(info)
                binding.apply {
                    assetRank.isVisible = true
                    titleView.rightExtraIb.isVisible = true
                    assetSymbol.text = info.symbol
                    assetName.text = info.name
                    assetRank.text = "#${info.marketCapRank}"
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

    @SuppressLint("SetTextI18n")
    private fun loadBalance(marketItem: MarketItem) {
        val changeUsd = BigDecimal(marketItem.priceChange24h)
        val isPositive = changeUsd >= BigDecimal.ZERO
        binding.apply {
            lifecycleScope.launch(CoroutineExceptionHandler { _, error ->
                Timber.e(error)
                balanceRl.isVisible = false
            }) {
                val ids = walletViewModel.findTokenIdsByCoinId(marketItem.coinId)
                val tokens = if (marketItem.coinId.isBlank()) {
                    walletViewModel.findAssetItemById(marketItem.assetIds!!.first())?.let {
                        listOf(it)
                    } ?: emptyList()
                } else {
                    walletViewModel.findTokensByCoinId(marketItem.coinId)
                }
                if (tokens.isNotEmpty()) {
                    val balances = tokens.sumOf { BigDecimal(it.balance) }
                    val price = BigDecimal(marketItem.currentPrice).multiply(BigDecimal(Fiats.getRate())).multiply(balances)
                    balance.text = "${balances.numberFormat8()} ${marketItem.symbol}"
                    value.text = try {
                        if (price == BigDecimal.ZERO) {
                            "≈ ${Fiats.getSymbol()}0.00"
                        } else {
                            "≈ ${Fiats.getSymbol()}${price.numberFormat2()}"
                        }
                    } catch (_: NumberFormatException) {
                        "≈ ${Fiats.getSymbol()}${price.numberFormat2()}"
                    }
                    priceRise.visibility = VISIBLE
                    currentRise = "${(BigDecimal(marketItem.priceChangePercentage24H)).numberFormat2()}%"
                    if (balances != BigDecimal.ZERO && marketItem.priceChangePercentage24H.isNotEmpty()) {
                        val change = changeUsd.multiply(balances).multiply(BigDecimal(Fiats.getRate()))
                        balanceChange.setQuoteText("${if (change >= BigDecimal.ZERO) "+" else "-"}${Fiats.getSymbol()}${change.priceFormat2().replace("-", "")} ($currentRise)", isPositive)
                        riseTitle.isVisible = true
                    } else {
                        balanceChange.setTextColor(requireContext().colorAttr(R.attr.text_assist))
                        balanceChange.text = "0.00%"
                        riseTitle.isVisible = false
                    }
                    priceRise.setQuoteTextWithBackgroud(currentRise, !marketItem.priceChangePercentage24H.startsWith("-"))
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
                            val nowTokens = if (marketItem.coinId.isBlank()) {
                                walletViewModel.findAssetItemById(marketItem.assetIds!!.first())?.let {
                                    listOf(it)
                                } ?: emptyList()
                            } else {
                                walletViewModel.findTokensByCoinId(marketItem.coinId)
                            }
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
        }
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