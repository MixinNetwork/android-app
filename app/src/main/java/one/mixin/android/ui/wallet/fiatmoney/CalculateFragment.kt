package one.mixin.android.ui.wallet.fiatmoney

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.PREF_ROUTE_BOT_PK
import one.mixin.android.Constants.AssetId.USDT_ASSET_ETH_ID
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.MixinResponseException
import one.mixin.android.api.request.RouteTickerRequest
import one.mixin.android.databinding.FragmentCalculateBinding
import one.mixin.android.db.web3.vo.notClassic
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormat8
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putString
import one.mixin.android.extension.shaking
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.setting.getCurrencyData
import one.mixin.android.ui.wallet.AssetListFixedBottomSheetDialogFragment
import one.mixin.android.ui.wallet.FiatListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.LoadingProgressDialogFragment
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.Keyboard
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode

@AndroidEntryPoint
class CalculateFragment : BaseFragment(R.layout.fragment_calculate) {
    companion object {
        const val TAG = "CalculateFragment"
        const val CALCULATE_STATE = "calculate_state"
        const val CURRENT_CURRENCY = "current_currency"
        const val CURRENT_ASSET_ID = "current_asset_id"
        const val ARGS_IS_WEB3 = "args_is_web3"
        const val ARGS_WALLET_ID_FOR_CALCULATE = "args_wallet_id_for_calculate"

        fun newInstance() = CalculateFragment()
    }

    private val binding by viewBinding(FragmentCalculateBinding::bind)
    private val fiatMoneyViewModel by viewModels<FiatMoneyViewModel>()
    private val web3ViewModel by viewModels<one.mixin.android.ui.home.web3.Web3ViewModel>()

    private val isWeb3 by lazy { requireArguments().getBoolean(ARGS_IS_WEB3, false) }
    private val walletIdForCalculate by lazy { requireArguments().getString(ARGS_WALLET_ID_FOR_CALCULATE) }

    private suspend fun initData() {
        var currencyName = getDefaultCurrency(requireContext(), getCurrencyData(requireContext().resources))
        val assetId = requireContext().defaultSharedPreferences.getString(CURRENT_ASSET_ID, null)

        if (assetId != null) {
            val currency = getCurrencyData(requireContext().resources).find { it.name == currencyName }
            val asset = fiatMoneyViewModel.findAssetsById(assetId)
            if (currency != null && asset != null) {
                updateUI(currency, asset)
            }
        } else {
            runCatching {
                loadingDialog.show(parentFragmentManager, LoadingProgressDialogFragment.TAG)
            }.onFailure {
                Timber.e(it)
            }
        }

        val routeProfile = (requireActivity() as WalletActivity).routeProfile
        if (routeProfile == null) {
            checkData()
            return
        }
        if (routeProfile.supportCurrencies.isEmpty()) {
            checkData()
            return
        }
        if (fiatMoneyViewModel.asset != null && fiatMoneyViewModel.currency != null) {
            return
        }
        val supportCurrencies = routeProfile.supportCurrencies
        currencyName = runCatching { getDefaultCurrency(requireContext(), supportCurrencies) }.getOrDefault("")

        fiatMoneyViewModel.currency = supportCurrencies.find {
            it.name == currencyName
        } ?: supportCurrencies.lastOrNull()
        fiatMoneyViewModel.asset =
            fiatMoneyViewModel.findAssetsByIds(routeProfile.supportAssetIds).let { list ->
                if (assetId == null) {
                    requireContext().defaultSharedPreferences.putString(CURRENT_ASSET_ID, list.firstOrNull()?.assetId)
                }
                list.find { it.assetId == assetId } ?: list.firstOrNull()
            }

        fiatMoneyViewModel.asset?.let { asset ->
            binding.assetIv.loadImage(asset.iconUrl)
            binding.assetName.text = asset.symbol
        }
        fiatMoneyViewModel.currency?.let { currency ->
            binding.flagIv.setImageResource(currency.flag)
            binding.fiatName.text = currency.name
        }

        if (fiatMoneyViewModel.calculateState == null) {
            fiatMoneyViewModel.calculateState =
                requireArguments().getParcelableCompat(
                    CALCULATE_STATE,
                    FiatMoneyViewModel.CalculateState::class.java,
                )
        }
        if (fiatMoneyViewModel.calculateState == null) {
            refresh()
        }
    }

    private var isLoading = false
    private fun setLoading(loading: Boolean) {
        isLoading = loading
        binding.fiatLoading.isVisible = isLoading
        binding.assetLoading.isVisible = isLoading
        binding.fiatExpandIv.isVisible = !loading
        binding.assetDesc.isVisible = !loading
        binding.fiatRl.isEnabled = !loading
        binding.assetRl.isEnabled = !loading
        if (!loading) {
            updateUI()
            runCatching {
                if (loadingDialog.isAdded) {
                    loadingDialog.dismiss()
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private val loadingDialog by lazy {
        LoadingProgressDialogFragment()
    }

    private suspend fun refresh(errorHandler: (() -> Unit)? = null) {
        val currency = fiatMoneyViewModel.currency ?: return
        val asset = fiatMoneyViewModel.asset ?: return
        setLoading(true)
        requestRouteAPI(
            invokeNetwork = {
                fiatMoneyViewModel.ticker(RouteTickerRequest(currency.name, asset.assetId))
            },
            defaultExceptionHandle = {
                ErrorHandler.handleError(it)
                errorHandler?.invoke()
            },
            defaultErrorHandle = {
                ErrorHandler.handleMixinError(it.errorCode, it.errorDescription)
                errorHandler?.invoke()
            },
            endBlock = {
                setLoading(false)
            },
            successBlock = {
                if (it.isSuccess) {
                    fiatMoneyViewModel.calculateState =
                        FiatMoneyViewModel.CalculateState(
                            minimum = it.data?.minimum?.toIntOrNull() ?: 0,
                            assetPrice = it.data?.assetPrice?.toFloatOrNull() ?: 0f,
                            feePercent = it.data?.feePercent?.toFloatOrNull() ?: 0f,
                        )
                }
            },
            requestSession = { fiatMoneyViewModel.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID)) },
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            binding.apply {
                titleView.leftIb.setOnClickListener {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
                titleView.rightIb.setOnClickListener {
                    context?.openUrl(Constants.HelpLink.CUSTOMER_SERVICE)
                }
                if (isWeb3) {
                    val wallet = walletIdForCalculate?.let { web3ViewModel.findWalletById(it) }
                    if (wallet != null && wallet.notClassic()) {
                        titleView.setSubTitle(
                            getString(R.string.Buy),
                            wallet.name.takeIf { it.isNotEmpty() } ?: getString(R.string.Common_Wallet)
                        )
                    } else {
                        titleView.setSubTitle(getString(R.string.Buy), getString(R.string.Common_Wallet))
                    }
                } else {
                    titleView.setSubTitle(
                        getString(R.string.Buy),
                        getString(R.string.Privacy_Wallet),
                        R.drawable.ic_wallet_privacy
                    )
                }
                assetRl.setOnClickListener {
                    if (isLoading) return@setOnClickListener
                    val routeProfile = (requireActivity() as WalletActivity).routeProfile
                    val supportAssetIds = routeProfile?.supportAssetIds ?: return@setOnClickListener
                    AssetListFixedBottomSheetDialogFragment.newInstance(
                        ArrayList(supportAssetIds),
                    ).setOnAssetClick { asset ->
                        this@CalculateFragment.lifecycleScope.launch {
                            val oldAsset = fiatMoneyViewModel.asset
                            fiatMoneyViewModel.asset = asset
                            requireContext().defaultSharedPreferences.putString(
                                CURRENT_ASSET_ID,
                                asset.assetId
                            )
                            refresh {
                                fiatMoneyViewModel.asset = oldAsset
                                requireContext().defaultSharedPreferences.putString(
                                    CURRENT_ASSET_ID,
                                    oldAsset!!.assetId
                                )
                                updateUI()
                            }
                            updateUI()
                        }
                    }.showNow(parentFragmentManager, AssetListFixedBottomSheetDialogFragment.TAG)
                }
                fiatRl.setOnClickListener {
                    if (isLoading) return@setOnClickListener
                    val routeProfile = (requireActivity() as WalletActivity).routeProfile
                    val supportCurrencies =
                        routeProfile?.supportCurrencies ?: return@setOnClickListener
                    FiatListBottomSheetDialogFragment.newInstance(
                        fiatMoneyViewModel.currency!!,
                        supportCurrencies
                    ).apply {
                        callback =
                            object : FiatListBottomSheetDialogFragment.Callback {
                                override fun onCurrencyClick(currency: Currency) {
                                    this@CalculateFragment.lifecycleScope.launch {
                                        val oldCurrency = fiatMoneyViewModel.currency
                                        fiatMoneyViewModel.currency = currency
                                        v = "0"
                                        requireContext().defaultSharedPreferences.putString(
                                            CURRENT_CURRENCY,
                                            currency.name
                                        )
                                        refresh {
                                            fiatMoneyViewModel.currency = oldCurrency!!
                                            v = "0"
                                            requireContext().defaultSharedPreferences.putString(
                                                CURRENT_CURRENCY,
                                                oldCurrency.name
                                            )
                                            updateUI()
                                        }
                                        updateUI()
                                    }
                                }
                            }
                    }.showNow(parentFragmentManager, FiatListBottomSheetDialogFragment.TAG)
                }
                keyboard.tipTitleEnabled = false
                keyboard.disableNestedScrolling()
                keyboard.setOnClickKeyboardListener(
                    object : Keyboard.OnClickKeyboardListener {
                        override fun onKeyClick(
                            position: Int,
                            value: String,
                        ) {
                            context?.tickVibrate()
                            if (position == 11) {
                                v =
                                    if (v == "0") {
                                        "0"
                                    } else if (v.length == 1) {
                                        "0"
                                    } else {
                                        v.substring(0, v.length - 1)
                                    }
                            } else {
                                if (v == "0" && value != ".") {
                                    v = value
                                } else if (isTwoDecimal(v)) {
                                    // do nothing
                                    return
                                } else if (value == "." && (v.contains(".") || AmountUtil.fullCurrency(
                                        fiatMoneyViewModel.currency!!.name
                                    ))
                                ) {
                                    // do nothing
                                    return
                                } else if (AmountUtil.illegal(
                                        v,
                                        fiatMoneyViewModel.currency!!.name
                                    )
                                ) {
                                    binding.primaryTv.shaking()
                                    return
                                } else {
                                    v += value
                                }
                            }
                            updateValue()
                        }

                        override fun onLongClick(
                            position: Int,
                            value: String,
                        ) {
                            context?.clickVibrate()
                        }
                    },
                )
                keyboard.initPinKeys(
                    requireContext(),
                    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "<<"),
                    force = true,
                    white = true,
                )
                continueVa.setOnClickListener {
                    val value =
                        if (v.endsWith(".")) {
                            v.substring(0, v.length)
                        } else {
                            v
                        }
                    val feePercent = fiatMoneyViewModel.calculateState?.feePercent ?: 0f
                    val assetPrice = fiatMoneyViewModel.calculateState?.assetPrice ?: 1f
                    val amount =
                        if (fiatMoneyViewModel.isReverse) {
                            getPayAmount(value.toFloat(), assetPrice, feePercent)
                        } else {
                            value
                        }
                    if (amount == null) {
                        toast("number error")
                    } else {
                        lifecycleScope.launch inner@{
                            if (viewDestroyed()) throw IllegalStateException("View has been destroyed")
                            try {
                                val asset = fiatMoneyViewModel.asset ?: throw IllegalStateException("Asset is null")
                                val destination = if (isWeb3) {
                                    val walletId = walletIdForCalculate ?: throw IllegalStateException("Wallet ID for calculate is null")
                                    web3ViewModel.getAddressesByChainId(walletId, asset.chainId)?.destination ?: throw IllegalStateException("Destination address is null for web3")
                                } else
                                    fiatMoneyViewModel.getAddressById(asset.chainId)?.destination ?: throw IllegalStateException("Destination address is null")
                                binding.continueVa.displayedChild = 1
                                val response = fiatMoneyViewModel.rampWebUrl(
                                    amount,
                                    asset.assetId,
                                    fiatMoneyViewModel.currency?.name ?: throw IllegalStateException("Currency name is null"),
                                    destination
                                )
                                if (response.isSuccess) {
                                    WebActivity.show(
                                        requireActivity(),
                                        response.data?.url ?: "",
                                        null
                                    )
                                } else {
                                    ErrorHandler.handleMixinError(response.errorCode, response.errorDescription)
                                }
                                binding.continueVa.displayedChild = 0
                            } catch (e: Exception) {
                                binding.continueVa.displayedChild = 0
                                ErrorHandler.handleError(e)
                            }
                        }
                    }
                }
                switchIv.setOnClickListener {
                    fiatMoneyViewModel.isReverse = !fiatMoneyViewModel.isReverse
                    updateUI()
                }
            }
            if (fiatMoneyViewModel.calculateState == null) {
                initData()
                updateUI()
            } else {
                binding.primaryTv.text = v
                updateUI()
            }
        }
    }

    private fun isTwoDecimal(string: String): Boolean {
        return string.matches(Regex("\\d+\\.\\d{2}"))
    }

    private var v = "0"

    @SuppressLint("SetTextI18n")
    private fun updateUI(currency: Currency? = null, asset: TokenItem? = null) {
        if (viewDestroyed()) return
        val currency = currency ?: fiatMoneyViewModel.currency ?: return
        val asset = asset ?: fiatMoneyViewModel.asset ?: return
        if (fiatMoneyViewModel.isReverse) {
            binding.apply {
                primaryUnit.text = asset.symbol
            }
        } else {
            binding.apply {
                primaryUnit.text = currency.name
            }
        }
        binding.flagIv.setImageResource(currency.flag)
        binding.assetIv.loadImage(asset.iconUrl)
        binding.fiatName.text = currency.name
        binding.assetName.text = asset.symbol
        binding.continueTv.text = "${getString(R.string.Buy)} ${asset.symbol}"
        updateValue(currency, asset)
    }

    @SuppressLint("SetTextI18n")
    private fun updateValue(currency: Currency? = null, asset: TokenItem? = null) {
        if (viewDestroyed()) return
        val currency = currency ?: fiatMoneyViewModel.currency ?: return
        val asset = asset ?: fiatMoneyViewModel.asset ?: return
        val state = fiatMoneyViewModel.calculateState
        if (state == null) {
            // Default state
            binding.primaryTv.text = "0"
            binding.minorTv.text = "0 ${asset.symbol}"
            return
        }
        val feePercent = fiatMoneyViewModel.calculateState?.feePercent ?: 0f
        if (!isAdded) return
        binding.apply {
            val value =
                if (v.endsWith(".")) {
                    v.substring(0, v.length)
                } else {
                    v
                }
            if (fiatMoneyViewModel.isReverse) {
                val currentValue = value.toFloat() * state.assetPrice * (1f + feePercent)
                if (value == "0") {
                    primaryTv.text = "0"
                    minorTv.text = "0 ${currency.name}"
                } else {
                    primaryTv.text = getNumberFormat(value)
                    minorTv.text =
                        "≈ ${getNumberFormat(String.format("%.2f", currentValue))} ${currency.name}"
                }
                continueVa.isEnabled =
                    currentValue >= state.minimum
                continueTv.isEnabled = continueVa.isEnabled
                info.setTextColor(requireContext().colorFromAttribute(R.attr.text_assist))
            } else {
                val currentValue = value.toFloat()
                if (value == "0") {
                    primaryTv.text = "0"
                    minorTv.text = "0 ${asset.symbol}"
                } else {
                    primaryTv.text = getNumberFormat(value)
                    minorTv.text =
                        "≈ ${BigDecimal((currentValue / state.assetPrice).toDouble()).numberFormat8()} ${asset.symbol}"
                }
                continueVa.isEnabled =
                    currentValue >= state.minimum
                continueTv.isEnabled = continueVa.isEnabled
                info.setTextColor(requireContext().colorFromAttribute(R.attr.text_assist))
            }
            updatePrimarySize()
            binding.info.text = getString(
                R.string.Value_info,
                state.minimum,
                currency.name,
            )
        }
    }

    private fun getNumberFormat(value: String): String {
        return value.numberFormat2().let {
            if (v.endsWith(".")) {
                "$it."
            } else if (v.endsWith(".00")) {
                "$it.00"
            } else if (v.endsWith(".0")) {
                "$it.0"
            } else {
                it
            }
        }
    }

    private fun updatePrimarySize() {
        if (viewDestroyed()) return
        binding.apply {
            val length = primaryTv.text.length
            val size =
                if (length <= 4) {
                    56f
                } else {
                    56f - 2 * (length - 4)
                }
            primaryTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        }
    }

    private fun getPayAmount(
        assetAmount: Float,
        assetPrice: Float,
        feePercent: Float,
    ): String {
        val merchant = assetAmount * assetPrice
        val total = merchant / (1 - feePercent)
        val fee = total - merchant
        val roundedFee = BigDecimal(fee.toDouble()).setScale(2, RoundingMode.UP)
        val payingAmount =
            roundedFee.plus(BigDecimal(merchant.toDouble())).setScale(2, RoundingMode.UP)
        return payingAmount.toPlainString()
    }

    private fun checkData() {
        lifecycleScope.launch {
            if (viewDestroyed()) return@launch
            setLoading(true)
            flow {
                emit(ROUTE_BOT_USER_ID)
            }.map { botId ->
                val key =
                    fiatMoneyViewModel.findBotPublicKey(
                        generateConversationId(
                            botId,
                            Session.getAccountId()!!,
                        ),
                        botId,
                    )
                if (!key.isNullOrEmpty()) {
                    MixinApplication.appContext.defaultSharedPreferences.putString(
                        PREF_ROUTE_BOT_PK,
                        key
                    )
                } else {
                    val sessionResponse =
                        fiatMoneyViewModel.fetchSessionsSuspend(listOf(botId))
                    if (sessionResponse.isSuccess) {
                        val sessionData = requireNotNull(sessionResponse.data)[0]
                        fiatMoneyViewModel.saveSession(
                            ParticipantSession(
                                generateConversationId(
                                    sessionData.userId,
                                    Session.getAccountId()!!,
                                ),
                                sessionData.userId,
                                sessionData.sessionId,
                                publicKey = sessionData.publicKey,
                            ),
                        )
                        MixinApplication.appContext.defaultSharedPreferences.putString(
                            PREF_ROUTE_BOT_PK,
                            sessionData.publicKey
                        )
                    } else {
                        throw MixinResponseException(
                            sessionResponse.errorCode,
                            sessionResponse.errorDescription,
                        )
                    }
                }
                botId
            }.map { _ ->
                val profileResponse =
                    requestRouteAPI(
                        invokeNetwork = { fiatMoneyViewModel.profile() },
                        defaultErrorHandle = {},
                        defaultExceptionHandle = {},
                        successBlock = { response ->
                           response
                        },
                        requestSession = { fiatMoneyViewModel.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID)) },
                    )
                if (profileResponse != null && profileResponse.isSuccess) {
                    val supportCurrencies =
                        getCurrencyData(requireContext().resources).filter {
                            profileResponse.data!!.currencies.contains(it.name)
                        }
                    val supportAssetIds = profileResponse.data!!.assetIds
                    val kycState = profileResponse.data!!.kycState
                    val hideGooglePay =
                        profileResponse.data!!.supportPayments.contains(Constants.RouteConfig.GOOGLE_PAY)
                            .not()
                    val routeProfile =
                        RouteProfile(kycState, hideGooglePay, supportCurrencies, supportAssetIds)
                    (requireActivity() as WalletActivity).routeProfile = routeProfile
                    routeProfile
                } else if (profileResponse != null) {
                    throw MixinResponseException(
                        profileResponse.errorCode,
                        profileResponse.errorDescription,
                    )
                } else {
                    throw IllegalStateException()
                }
            }.map { routeProfile ->
                fiatMoneyViewModel.syncNoExistAsset(routeProfile.supportAssetIds)
                routeProfile
            }.map { routeProfile ->
                val assetId =
                    requireContext().defaultSharedPreferences.getString(
                        CURRENT_ASSET_ID,
                        USDT_ASSET_ETH_ID,
                    ) ?: routeProfile.supportAssetIds.first()
                val currency = getDefaultCurrency(requireContext(), routeProfile.supportCurrencies)
                val tickerResponse =
                    fiatMoneyViewModel.ticker(
                        RouteTickerRequest(
                            currency,
                            assetId,
                        ),
                    )
                if (tickerResponse.isSuccess) {
                    val state =
                        FiatMoneyViewModel.CalculateState(
                            minimum =
                                tickerResponse.data!!.minimum.toIntOrNull()
                                    ?: 0,
                            assetPrice =
                                tickerResponse.data!!.assetPrice.toFloatOrNull()
                                    ?: 0f,
                            feePercent =
                                tickerResponse.data!!.feePercent.toFloatOrNull()
                                    ?: 0f,
                        )
                    state
                } else {
                    throw MixinResponseException(
                        tickerResponse.errorCode,
                        tickerResponse.errorDescription,
                    )
                }
            }.catch { e ->
                activity?.finish()
            }.collectLatest { state ->
                fiatMoneyViewModel.calculateState = state
                setLoading(false)
                initData()
                updateUI()
            }
        }
    }
}
