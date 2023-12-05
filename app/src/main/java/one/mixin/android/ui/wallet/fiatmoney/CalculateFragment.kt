package one.mixin.android.ui.wallet.fiatmoney

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.sumsub.sns.core.SNSMobileSDK
import com.sumsub.sns.core.data.listener.TokenExpirationHandler
import com.sumsub.sns.core.data.model.SNSCompletionResult
import com.sumsub.sns.core.data.model.SNSException
import com.sumsub.sns.core.data.model.SNSSDKState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.AssetId.USDT_ASSET_ID
import one.mixin.android.Constants.RouteConfig.ROUTE_BOT_USER_ID
import one.mixin.android.R
import one.mixin.android.api.MixinResponseException
import one.mixin.android.api.request.RouteTickerRequest
import one.mixin.android.databinding.FragmentCalculateBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.navigate
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
import one.mixin.android.ui.setting.AppearanceFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.setting.getCurrencyData
import one.mixin.android.ui.setting.getLanguagePos
import one.mixin.android.ui.wallet.AssetListFixedBottomSheetDialogFragment
import one.mixin.android.ui.wallet.FiatListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.IdentityFragment.Companion.ARGS_KYC_STATE
import one.mixin.android.ui.wallet.IdentityFragment.Companion.ARGS_TOKEN
import one.mixin.android.ui.wallet.LoadingProgressDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.wallet.fiatmoney.OrderConfirmFragment.Companion.ARGS_AMOUNT
import one.mixin.android.ui.wallet.fiatmoney.OrderConfirmFragment.Companion.ARGS_CURRENCY
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.isFollowSystem
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.sumsub.KycState
import one.mixin.android.widget.Keyboard
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

@AndroidEntryPoint
class CalculateFragment : BaseFragment(R.layout.fragment_calculate) {
    companion object {
        const val TAG = "CalculateFragment"
        const val CALCULATE_STATE = "calculate_state"
        const val CURRENT_CURRENCY = "current_currency"
        const val CURRENT_ASSET_ID = "current_asset_id"

        fun newInstance() = CalculateFragment()
    }

    private val binding by viewBinding(FragmentCalculateBinding::bind)
    private val fiatMoneyViewModel by viewModels<FiatMoneyViewModel>()

    private suspend fun initData() {
        if ((requireActivity() as WalletActivity).supportCurrencies.isEmpty()) {
            checkData()
            return
        }

        if (fiatMoneyViewModel.asset != null && fiatMoneyViewModel.currency != null) {
            return
        }
        val supportCurrencies = (requireActivity() as WalletActivity).supportCurrencies
        val currencyName = getDefaultCurrency(requireContext(), supportCurrencies)
        val assetId =
            requireContext().defaultSharedPreferences.getString(
                CURRENT_ASSET_ID,
                USDT_ASSET_ID,
            )

        fiatMoneyViewModel.currency = supportCurrencies.find {
            it.name == currencyName
        } ?: supportCurrencies.lastOrNull()
        fiatMoneyViewModel.asset =
            fiatMoneyViewModel.findAssetsByIds((requireActivity() as WalletActivity).supportAssetIds).let { list ->
                list.find { it.assetId == assetId } ?: list.firstOrNull()
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

    private suspend fun refresh() {
        val currency = fiatMoneyViewModel.currency ?: return
        val asset = fiatMoneyViewModel.asset ?: return
        showLoading()
        requestRouteAPI(
            invokeNetwork = {
                fiatMoneyViewModel.ticker(RouteTickerRequest(0, currency.name, asset.assetId))
            },
            endBlock = {
                dismissLoading()
            },
            successBlock = {
                if (it.isSuccess) {
                    fiatMoneyViewModel.calculateState =
                        FiatMoneyViewModel.CalculateState(
                            minimum = it.data?.minimum?.toIntOrNull() ?: 0,
                            maximum = it.data?.maximum?.toIntOrNull() ?: 0,
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
                titleView.setSubTitle(getString(R.string.Buy), "")
                titleView.rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.EMERGENCY) }
                assetRl.setOnClickListener {
                    AssetListFixedBottomSheetDialogFragment.newInstance(
                        ArrayList((requireActivity() as WalletActivity).supportAssetIds),
                    ).setOnAssetClick { asset ->
                        this@CalculateFragment.lifecycleScope.launch {
                            fiatMoneyViewModel.asset = asset
                            requireContext().defaultSharedPreferences.putString(CURRENT_ASSET_ID, asset.assetId)
                            refresh()
                            updateUI()
                        }
                    }.showNow(parentFragmentManager, AssetListFixedBottomSheetDialogFragment.TAG)
                }
                fiatRl.setOnClickListener {
                    FiatListBottomSheetDialogFragment.newInstance(fiatMoneyViewModel.currency!!, (requireActivity() as WalletActivity).supportCurrencies).apply {
                        callback =
                            object : FiatListBottomSheetDialogFragment.Callback {
                                override fun onCurrencyClick(currency: Currency) {
                                    this@CalculateFragment.lifecycleScope.launch {
                                        fiatMoneyViewModel.currency = currency
                                        v = "0"
                                        requireContext().defaultSharedPreferences.putString(CURRENT_CURRENCY, currency.name)
                                        refresh()
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
                                    // do noting
                                    return
                                } else if (value == "." && (v.contains(".") || AmountUtil.fullCurrency(fiatMoneyViewModel.currency!!.name))) {
                                    // do noting
                                    return
                                } else if (AmountUtil.illegal(v, fiatMoneyViewModel.currency!!.name)) {
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
                    checkKyc {
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
                                AmountUtil.toAmount(
                                    getPayAmount(value.toFloat(), assetPrice, feePercent),
                                    fiatMoneyViewModel.currency!!.name,
                                )
                            } else {
                                AmountUtil.toAmount(value, fiatMoneyViewModel.currency!!.name)
                            }
                        if (amount == null) {
                            toast("number error")
                        } else {
                            lifecycleScope.launch inner@{
                                if (viewDestroyed()) return@inner
                                try {
                                    binding.continueVa.displayedChild = 1
                                    initSafeBox()
                                    binding.continueVa.displayedChild = 0
                                    view.navigate(
                                        R.id.action_wallet_calculate_to_payment,
                                        Bundle().apply {
                                            putParcelable(ARGS_ASSET, fiatMoneyViewModel.asset)
                                            putParcelable(
                                                ARGS_CURRENCY,
                                                fiatMoneyViewModel.currency,
                                            )
                                            putLong(ARGS_AMOUNT, amount)
                                        },
                                    )
                                } catch (e: Exception) {
                                    ErrorHandler.handleError(e)
                                }
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

    private suspend fun initSafeBox() {
        fiatMoneyViewModel.instruments().data?.let { cards ->
            if (cards.isNotEmpty()) {
                fiatMoneyViewModel.initSafeBox(cards)
            }
        }
    }

    private fun isTwoDecimal(string: String): Boolean {
        return string.matches(Regex("\\d+\\.\\d{2}"))
    }

    private var v = "0"

    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        if (viewDestroyed()) return
        val currency = fiatMoneyViewModel.currency ?: return
        val asset = fiatMoneyViewModel.asset ?: return
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
        updateValue()
    }

    private val loading by lazy {
        LoadingProgressDialogFragment()
    }

    private var loadingShown = false

    private fun showLoading() {
        if (viewDestroyed()) return
        if (!loadingShown) {
            loadingShown = true
            loading.show(parentFragmentManager, LoadingProgressDialogFragment.TAG)
        }
    }

    private fun dismissLoading() {
        if (viewDestroyed()) return
        if (loadingShown) {
            loadingShown = false
            loading.dismiss()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateValue() {
        if (viewDestroyed()) return
        val currency = fiatMoneyViewModel.currency ?: return
        val asset = fiatMoneyViewModel.asset ?: return
        val state = fiatMoneyViewModel.calculateState ?: return
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
                continueVa.isEnabled = currentValue >= state.minimum && currentValue <= state.maximum
                continueTv.isEnabled = continueVa.isEnabled
                if (currentValue > state.maximum) {
                    info.setTextColor(requireContext().getColorStateList(R.color.colorRed))
                } else {
                    info.setTextColor(requireContext().colorFromAttribute(R.attr.text_minor))
                }
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
                continueVa.isEnabled = currentValue >= state.minimum && currentValue <= state.maximum
                continueTv.isEnabled = continueVa.isEnabled
                if (currentValue > state.maximum) {
                    info.setTextColor(requireContext().getColorStateList(R.color.colorRed))
                } else {
                    info.setTextColor(requireContext().colorFromAttribute(R.attr.text_minor))
                }
            }
            updatePrimarySize()
            binding.info.text = getString(R.string.Value_info, state.minimum, currency.name, state.maximum, currency.name)
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

    private fun checkKyc(onSuccess: () -> Unit) =
        lifecycleScope.launch {
            if ((requireActivity() as WalletActivity).kycState == KycState.SUCCESS.value || (requireActivity() as WalletActivity).kycState == KycState.IGNORE.value) {
                onSuccess.invoke()
                return@launch
            }
            binding.continueVa.displayedChild = 1
            requestRouteAPI(
                invokeNetwork = {
                    fiatMoneyViewModel.token()
                },
                endBlock = {
                    binding.continueVa.displayedChild = 0
                },
                successBlock = { resp ->
                    val tokenResponse = requireNotNull(resp.data)
                    when (tokenResponse.state) {
                        KycState.INITIAL.value -> {
                            val token = requireNotNull(tokenResponse.token) { "required token can not be null" }
                            presentSDK(token)
                        }

                        KycState.PENDING.value, KycState.RETRY.value, KycState.BLOCKED.value -> {
                            val token =
                                requireNotNull(tokenResponse.token) { "required token can not be null" }
                            view?.navigate(
                                R.id.action_wallet_calculate_to_identity,
                                Bundle().apply {
                                    putString(ARGS_TOKEN, token)
                                    putString(ARGS_KYC_STATE, tokenResponse.state)
                                },
                            )
                        }

                        KycState.SUCCESS.value -> {
                            onSuccess()
                        }

                        else -> {
                            toast("Unknown kyc state: ${tokenResponse.state}")
                        }
                    }
                },
                requestSession = { fiatMoneyViewModel.fetchSessionsSuspend(listOf(ROUTE_BOT_USER_ID)) },
            )
        }

    private fun presentSDK(accessToken: String) {
        val tokenExpirationHandler =
            object : TokenExpirationHandler {
                override fun onTokenExpired(): String? {
                    return try {
                        fiatMoneyViewModel.callSumsubToken().execute().body()?.data?.token
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        val onSDKStateChangedHandler: (SNSSDKState, SNSSDKState) -> Unit = { newState, prevState ->
            Timber.e("onSDKStateChangedHandler: $prevState -> $newState")

            when (newState) {
                is SNSSDKState.Ready -> Timber.e("SDK is ready")
                is SNSSDKState.Failed -> {
                    when (newState) {
                        is SNSSDKState.Failed.ApplicantNotFound -> Timber.e(newState.message)
                        is SNSSDKState.Failed.ApplicantMisconfigured -> Timber.e(newState.message)
                        is SNSSDKState.Failed.InitialLoadingFailed ->
                            Timber.e(
                                newState.exception,
                                "Initial loading error",
                            )

                        is SNSSDKState.Failed.InvalidParameters -> Timber.e(newState.message)
                        is SNSSDKState.Failed.NetworkError ->
                            Timber.e(
                                newState.exception,
                                newState.message,
                            )

                        is SNSSDKState.Failed.Unauthorized ->
                            Timber.e(
                                newState.exception,
                                "Invalid token or a token can't be refreshed by the SDK. Please, check your token expiration handler",
                            )

                        is SNSSDKState.Failed.Unknown ->
                            Timber.e(
                                newState.exception,
                                "Unknown error",
                            )
                    }
                }

                is SNSSDKState.Initial -> Timber.e("No verification steps are passed yet")
                is SNSSDKState.Incomplete -> Timber.e("Some but not all verification steps are passed over")
                is SNSSDKState.Pending -> Timber.e("Verification is in pending state")
                is SNSSDKState.FinallyRejected -> Timber.e("Applicant has been finally rejected")
                is SNSSDKState.TemporarilyDeclined -> Timber.e("Applicant has been declined temporarily")
                is SNSSDKState.Approved -> Timber.e("Applicant has been approved")
                else -> Timber.e("Unknown")
            }
        }

        val onSDKErrorHandler: (SNSException) -> Unit = { exception ->
            Timber.e("The SDK throws an exception. Exception: $exception")

            when (exception) {
                is SNSException.Api -> Timber.e("Api exception. ${exception.description}")
                is SNSException.Network -> Timber.e(exception, "Network exception.")
                is SNSException.Unknown -> Timber.e(exception, "Unknown exception.")
            }
        }

        val onSDKCompletedHandler: (SNSCompletionResult, SNSSDKState) -> Unit = { result, _ ->
            when (result) {
                is SNSCompletionResult.SuccessTermination -> Timber.e("The SDK finished successfully")
                is SNSCompletionResult.AbnormalTermination -> Timber.e(result.exception, "The SDK got closed because of errors")
                else -> Timber.e("Unknown")
            }
        }

        val snsSdk =
            SNSMobileSDK.Builder(requireActivity())
                .withHandlers(onStateChanged = onSDKStateChangedHandler, onError = onSDKErrorHandler, onCompleted = onSDKCompletedHandler)
                .withAccessToken(accessToken, onTokenExpiration = tokenExpirationHandler)
                .withLocale(
                    if (isFollowSystem()) {
                        Locale.getDefault()
                    } else {
                        val languagePos = getLanguagePos()
                        val selectedLang =
                            when (languagePos) {
                                AppearanceFragment.POS_SIMPLIFY_CHINESE -> Locale.SIMPLIFIED_CHINESE.language
                                AppearanceFragment.POS_TRADITIONAL_CHINESE -> Locale.TRADITIONAL_CHINESE.language
                                AppearanceFragment.POS_SIMPLIFY_JAPANESE -> Locale.JAPANESE.language
                                AppearanceFragment.POS_RUSSIAN -> Constants.Locale.Russian.Language
                                AppearanceFragment.POS_INDONESIA -> Constants.Locale.Indonesian.Language
                                AppearanceFragment.POS_Malay -> Constants.Locale.Malay.Language
                                AppearanceFragment.POS_Spanish -> Constants.Locale.Spanish.Language
                                else -> Locale.US.language
                            }
                        val selectedCountry =
                            when (languagePos) {
                                AppearanceFragment.POS_SIMPLIFY_CHINESE -> Locale.SIMPLIFIED_CHINESE.country
                                AppearanceFragment.POS_TRADITIONAL_CHINESE -> Locale.TRADITIONAL_CHINESE.country
                                AppearanceFragment.POS_SIMPLIFY_JAPANESE -> Locale.JAPANESE.country
                                AppearanceFragment.POS_RUSSIAN -> Constants.Locale.Russian.Country
                                AppearanceFragment.POS_INDONESIA -> Constants.Locale.Indonesian.Country
                                AppearanceFragment.POS_Malay -> Constants.Locale.Malay.Country
                                AppearanceFragment.POS_Spanish -> Constants.Locale.Spanish.Country
                                else -> Locale.US.country
                            }
                        Locale(selectedLang, selectedCountry)
                    },
                )
                .build()

        snsSdk.launch()
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
        val payingAmount = roundedFee.plus(BigDecimal(merchant.toDouble())).setScale(2, RoundingMode.UP)
        return payingAmount.toPlainString()
    }

    private fun checkData() {
        lifecycleScope.launch {
            if (viewDestroyed()) return@launch
            showLoading()
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
                    Session.routePublicKey = key
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
                        Session.routePublicKey = sessionData.publicKey
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
                    fiatMoneyViewModel.profile()
                if (profileResponse.isSuccess) {
                    val walletActivity = (requireActivity() as WalletActivity)
                    walletActivity.apply {
                        supportCurrencies =
                            getCurrencyData(requireContext().resources).filter {
                                profileResponse.data!!.currencies.contains(it.name)
                            }
                        supportAssetIds = profileResponse.data!!.assetIds
                        kycState = profileResponse.data!!.kycState
                        hideGooglePay =
                            profileResponse.data!!.supportPayments.contains(Constants.RouteConfig.GOOGLE_PAY)
                                .not()
                    }
                    Pair(walletActivity.supportCurrencies, walletActivity.supportAssetIds)
                } else {
                    throw MixinResponseException(
                        profileResponse.errorCode,
                        profileResponse.errorDescription,
                    )
                }
            }.map { data ->
                val (_, assetIds) = data
                fiatMoneyViewModel.syncNoExistAsset(assetIds)
                data
            }.map { data ->
                val (supportCurrencies, assetIds) = data
                val assetId =
                    requireContext().defaultSharedPreferences.getString(
                        CURRENT_ASSET_ID,
                        USDT_ASSET_ID,
                    ) ?: assetIds.first()
                val currency = getDefaultCurrency(requireContext(), supportCurrencies)
                val tickerResponse =
                    fiatMoneyViewModel.ticker(
                        RouteTickerRequest(
                            0,
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
                            maximum =
                                tickerResponse.data!!.maximum.toIntOrNull()
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
                dismissLoading()
                initData()
                updateUI()
            }
        }
    }
}
