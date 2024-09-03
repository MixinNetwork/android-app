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
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDetailsMarketBinding
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.marketPriceFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.extension.textColorResource
import one.mixin.android.extension.toast
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshMarketJob
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.market.Market
import one.mixin.android.ui.home.web3.market.ChooseTokensBottomSheetDialogFragment
import one.mixin.android.ui.wallet.AllTransactionsFragment.Companion.ARGS_TOKEN
import one.mixin.android.util.getChainName
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.market.MarketItem
import one.mixin.android.vo.safe.TokenItem
import timber.log.Timber
import java.math.BigDecimal
import java.util.ArrayList
import javax.inject.Inject

@AndroidEntryPoint
class MarketDetailsFragment : BaseFragment(R.layout.fragment_details_market) {
    companion object {
        const val TAG = "MarketDetailsFragment"
        const val ARGS_MARKET = "args_market"
    }

    private val binding by viewBinding(FragmentDetailsMarketBinding::bind)

    @Inject
    lateinit var jobManager: MixinJobManager

    private val walletViewModel by viewModels<WalletViewModel>()

    private var asset: TokenItem? = null
    private var marketItem: MarketItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        asset = requireArguments().getParcelableCompat(ARGS_TOKEN, TokenItem::class.java)
        marketItem = requireArguments().getParcelableCompat(ARGS_MARKET, MarketItem::class.java)
    }

    private val typeState = mutableStateOf("1D")

    @SuppressLint("SetTextI18n", "DefaultLocale")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val changeUsd = BigDecimal(asset?.changeUsd ?: marketItem?.priceChange24h ?: "0")
        val isPositive = changeUsd > BigDecimal.ZERO
        (asset?.assetId ?: marketItem?.coinId)?.let {
            jobManager.addJobInBackground(RefreshMarketJob(it))
        }
        binding.apply {
            titleView.apply {
                asset.let { asset ->
                    if (asset != null) {
                        val sub = getChainName(asset.chainId, asset.chainName, asset.assetKey)
                        if (sub != null)
                            setSubTitle(asset.name, sub)
                        else
                            titleTv.text = asset.name
                    } else {
                        setSubTitle(marketItem?.symbol ?: "", marketItem?.name ?: "")
                    }
                }
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

            asset.let { asset ->
                if (asset != null) {
                    chainTitle.isVisible = true
                    chain.isVisible = true
                    contactAddressTitle.isVisible = true
                    address.isVisible = true
                    addressSub.isVisible = true
                    balanceRl.isVisible = true
                    icon.loadToken(asset)
                    balanceRl.setOnClickListener {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                    balance.text = "${asset.balance} ${asset.symbol}"
                    chain.text = asset.chainName
                    address.text = asset.assetKey
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
                    value.text = try {
                        if (asset.fiat().toFloat() == 0f) {
                            "≈ ${Fiats.getSymbol()}0.00"
                        } else {
                            "≈ ${Fiats.getSymbol()}${asset.fiat().numberFormat2()}"
                        }
                    } catch (ignored: NumberFormatException) {
                        "≈ ${Fiats.getSymbol()}${asset.fiat().numberFormat2()}"
                    }
                    name.text = asset.name
                    symbol.text = asset.symbol
                } else {
                    chainTitle.isVisible = false
                    chain.isVisible = false
                    contactAddressTitle.isVisible = false
                    address.isVisible = false
                    addressSub.isVisible = false
                    name.text = marketItem?.name
                    symbol.text = marketItem?.symbol
                    icon.loadToken(marketItem!!)
                    lifecycleScope.launch(CoroutineExceptionHandler { _, error ->
                        Timber.e(error)
                        balanceRl.isVisible = false
                    }) {
                        val ids = walletViewModel.findTokenIdsByCoinId(marketItem!!.coinId)
                        val tokens = walletViewModel.findTokensByCoinId(marketItem!!.coinId)
                        if (ids.isNotEmpty()) {
                            val balances = tokens.sumOf { BigDecimal(it.balance) }
                            val price = BigDecimal(marketItem!!.currentPrice).multiply(BigDecimal(Fiats.getRate())).multiply(balances)
                            balance.text = "$balances ${marketItem?.symbol}"
                            value.text = try {
                                if (price == BigDecimal.ZERO) {
                                    "≈ ${Fiats.getSymbol()}0.00"
                                } else {
                                    "≈ ${Fiats.getSymbol()}${price.numberFormat2()}"
                                }
                            } catch (ignored: NumberFormatException) {
                                "≈ ${Fiats.getSymbol()}${price.numberFormat2()}"
                            }
                            balanceRl.setOnClickListener {
                                lifecycleScope.launch {
                                    if (ids.size >= tokens.size) {
                                        val dialog =
                                            indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
                                                setCancelable(false)
                                            }
                                        dialog.show()
                                        walletViewModel.syncNoExistAsset(ids.subtract(tokens.map { it.assetId }.toSet()).toList())
                                        dialog.dismiss()
                                    }
                                    ChooseTokensBottomSheetDialogFragment.newInstance(ArrayList<TokenItem>().apply { addAll(tokens) })
                                        .apply {
                                            callback = { token ->
                                                activity?.let { WalletActivity.showWithToken(it, token, WalletActivity.Destination.Transactions) }
                                            }
                                        }
                                        .show(parentFragmentManager, ChooseTokensBottomSheetDialogFragment.TAG)
                                }
                            }
                            balanceRl.isVisible = true
                        } else {
                            balanceRl.isVisible = false
                        }
                    }
                }
            }

            market.setContent {
                Market(typeState.value, asset?.assetId ?: marketItem?.coinId!!, { percentageChange ->
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
                })
            }
        }

        if (asset != null) {
            walletViewModel.marketById(asset?.assetId!!)
        } else {
            walletViewModel.marketByCoinId(marketItem?.coinId!!)
        }.observe(this.viewLifecycleOwner) { info ->
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
                        circulationSupply.text = "${info.circulatingSupply.numberFormat8()} ${asset?.symbol ?: marketItem?.symbol}"
                    }

                    if (info.marketCap == "0" || info.marketCap.isBlank()) {
                        marketCap.text = getString(R.string.N_A)
                        marketCap.setTextColor(textAssist)
                    } else {
                        marketCap.setTextColor(textPrimary)
                        marketCap.text = capFormat(info.marketCap, BigDecimal(Fiats.getRate()), Fiats.getSymbol())
                    }
                    totalSupply.text = "${info.totalSupply.numberFormat8()} ${asset?.symbol ?: marketItem?.symbol}"

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