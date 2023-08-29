package one.mixin.android.ui.wallet.fiatmoney

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.AssetId.USDT_ASSET_ID
import one.mixin.android.Constants.GOOGLE_PAY
import one.mixin.android.Constants.ROUTE_API_BOT_USER_ID
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.RouteTickerRequest
import one.mixin.android.databinding.FragmentCalculateBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.navigate
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.putString
import one.mixin.android.extension.shaking
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.setting.getCurrencyData
import one.mixin.android.ui.wallet.AssetListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.FiatListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.IdentityFragment.Companion.ARGS_IS_RETRY
import one.mixin.android.ui.wallet.IdentityVerificationStateBottomSheetDialogFragment
import one.mixin.android.ui.wallet.IdentityVerificationStateBottomSheetDialogFragment.Companion.ARGS_TOKEN
import one.mixin.android.ui.wallet.LoadingProgressDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.ui.wallet.fiatmoney.OrderConfirmFragment.Companion.ARGS_AMOUNT
import one.mixin.android.ui.wallet.fiatmoney.OrderConfirmFragment.Companion.ARGS_CURRENCY
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.sumsub.KycState
import one.mixin.android.widget.Keyboard

@AndroidEntryPoint
class CalculateFragment : BaseFragment(R.layout.fragment_calculate) {
    companion object {
        const val TAG = "CalculateFragment"
        private const val CURRENT_CURRENCY = "current_currency"
        private const val CURRENT_ASSET_ID = "current_asset_id"
        fun newInstance() = CalculateFragment()
    }

    private val binding by viewBinding(FragmentCalculateBinding::bind)
    private val fiatMoneyViewModel by viewModels<FiatMoneyViewModel>()

    private suspend fun initData() {
        if (fiatMoneyViewModel.asset != null && fiatMoneyViewModel.currency != null) {
            return
        }
        getProfiles()
        val currencyList = getCurrencyData(requireContext().resources)
        val currencyName = requireContext().defaultSharedPreferences.getString(
            CURRENT_CURRENCY,
            Session.getFiatCurrency(),
        )
        val assetId = requireContext().defaultSharedPreferences.getString(
            CURRENT_ASSET_ID,
            USDT_ASSET_ID,
        )
        fiatMoneyViewModel.currency = currencyList.find {
            it.name == currencyName
        } ?: currencyList.first()
        fiatMoneyViewModel.asset = fiatMoneyViewModel.findAssetsByIds(fiatMoneyViewModel.supportAssetIds).let { list ->
            list.find { it.assetId == assetId } ?: list.first()
        }
    }

    private suspend fun getProfiles() {
        showLoading()
        handleMixinResponse(
            invokeNetwork = {
                fiatMoneyViewModel.profile()
            },
            successBlock = {
                if (it.isSuccess) {
                    fiatMoneyViewModel.kycEnable = it.data?.kycEnable ?: true
                    fiatMoneyViewModel.supportCurrency = it.data?.currencies ?: emptyList()
                    fiatMoneyViewModel.supportAssetIds = it.data?.assetIds ?: emptyList()
                    (requireActivity() as WalletActivity).hideGooglePay = it.data?.supportPayments?.contains(GOOGLE_PAY)?.not() ?: false
                    (requireActivity() as WalletActivity).buyEnable = it.data?.buyEnable ?: true
                } else {
                    fiatMoneyViewModel.kycEnable = true
                }
            },
        )
    }

    private suspend fun refreshBotPublicKey() {
        handleMixinResponse(
            invokeNetwork = {
                fiatMoneyViewModel.fetchSessionsSuspend(listOf(ROUTE_API_BOT_USER_ID))
            },
            successBlock = { resp ->
                defaultSharedPreferences.putString(Constants.Account.PREF_CHECKOUT_BOT_PUBLIC_KEY, requireNotNull(resp.data)[0].publicKey)
            },
        )
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
                    fiatMoneyViewModel.calculateState = FiatMoneyViewModel.CalculateState(
                        minimum = it.data?.minimum?.toIntOrNull() ?: 0,
                        maximum = it.data?.maximum?.toIntOrNull() ?: 0,
                        fiatPrice = it.data?.price?.toFloatOrNull() ?: 0f,
                    )
                }
            },
            requestSession = { fiatMoneyViewModel.fetchSessionsSuspend(listOf(ROUTE_API_BOT_USER_ID)) },
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            binding.apply {
                titleView.leftIb.setOnClickListener {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
                titleView.setSubTitle(getString(R.string.Buy), "")
                titleView.rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.EMERGENCY) }
                assetRl.setOnClickListener {
                    AssetListBottomSheetDialogFragment.newInstance(
                        false,
                        ArrayList(fiatMoneyViewModel.supportAssetIds),
                    ).setOnAssetClick { asset ->
                        fiatMoneyViewModel.asset = asset
                        requireContext().defaultSharedPreferences.putString(CURRENT_ASSET_ID, asset.assetId)
                        updateUI()
                    }.showNow(parentFragmentManager, AssetListBottomSheetDialogFragment.TAG)
                }
                fiatRl.setOnClickListener {
                    FiatListBottomSheetDialogFragment.newInstance(fiatMoneyViewModel.currency!!).apply {
                        callback = object : FiatListBottomSheetDialogFragment.Callback {
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
                keyboard.setOnClickKeyboardListener(
                    object : Keyboard.OnClickKeyboardListener {
                        override fun onKeyClick(position: Int, value: String) {
                            context?.tickVibrate()
                            if (position == 11) {
                                v = if (v == "0") {
                                    "0"
                                } else if (v.length == 1) {
                                    "0"
                                } else {
                                    v.substring(0, v.length - 1)
                                }
                            } else {
                                if (v == "0" && value != ".") {
                                    v = value
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

                        override fun onLongClick(position: Int, value: String) {
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
                        val amount = AmountUtil.toAmount(v, fiatMoneyViewModel.currency!!.name)
                        if (amount == null) {
                            toast("number error")
                        } else {
                            view.navigate(
                                R.id.action_wallet_calculate_to_payment,
                                Bundle().apply {
                                    putParcelable(ARGS_ASSET, fiatMoneyViewModel.asset)
                                    putParcelable(ARGS_CURRENCY, fiatMoneyViewModel.currency)
                                    putInt(ARGS_AMOUNT, amount)
                                },
                            )
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
                refresh()
                updateUI()
            } else {
                binding.primaryTv.text = v
                updateUI()
            }
            refreshBotPublicKey()
        }
    }

    private var v = "0"

    private fun updateUI() {
        if (!isAdded) return
        val currency = fiatMoneyViewModel.currency ?: return
        val asset = fiatMoneyViewModel.asset ?: return
        if (fiatMoneyViewModel.isReverse) {
            binding.apply {
                fiatName.text = currency.name
                assetName.text = asset.symbol
                primaryUnit.text = asset.symbol
            }
        } else {
            binding.apply {
                fiatName.text = currency.name
                assetName.text = asset.symbol
                primaryUnit.text = currency.name
            }
        }
        updateValue()
    }

    private val loading by lazy {
        LoadingProgressDialogFragment()
    }

    private var loadingShown = false
    private fun showLoading() {
        if (!loadingShown) {
            loadingShown = true
            loading.show(parentFragmentManager, LoadingProgressDialogFragment.TAG)
        }
    }

    private fun dismissLoading() {
        if (loadingShown) {
            loadingShown = false
            loading.dismiss()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateValue() {
        val currency = fiatMoneyViewModel.currency ?: return
        val asset = fiatMoneyViewModel.asset ?: return
        val state = fiatMoneyViewModel.calculateState ?: return
        if (!isAdded) return
        binding.apply {
            val value = if (v.endsWith(".")) {
                v.substring(0, v.length)
            } else {
                v
            }
            if (fiatMoneyViewModel.isReverse) {
                val currentValue = value.toFloat() / state.fiatPrice
                if (value == "0") {
                    primaryTv.text = "0"
                    minorTv.text = "0 ${currency.name}"
                } else {
                    primaryTv.text = value
                    minorTv.text =
                        "≈ ${String.format("%.2f", currentValue)} ${currency.name}"
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
                    primaryTv.text = value
                    minorTv.text =
                        "≈ ${String.format("%.2f", currentValue * state.fiatPrice)} ${asset.symbol}"
                }
                continueVa.isEnabled = currentValue >= state.minimum && currentValue <= state.maximum
                continueTv.isEnabled = continueVa.isEnabled
                if (currentValue > state.maximum) {
                    info.setTextColor(requireContext().getColorStateList(R.color.colorRed))
                } else {
                    info.setTextColor(requireContext().colorFromAttribute(R.attr.text_minor))
                }
            }
            binding.info.text = getString(R.string.Value_info, state.minimum, currency.name, state.maximum, currency.name)
        }
    }

    private fun checkKyc(onSuccess: () -> Unit) = lifecycleScope.launch {
        if (!fiatMoneyViewModel.kycEnable) {
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
                        val token =
                            requireNotNull(tokenResponse.token) { "required token can not be null" }
                        view?.navigate(
                            R.id.action_wallet_calculate_to_identity,
                            Bundle().apply {
                                putString(ARGS_TOKEN, token)
                                putBoolean(ARGS_IS_RETRY, false)
                            },
                        )
                    }

                    KycState.PENDING.value, KycState.RETRY.value, KycState.BLOCKED.value -> {
                        IdentityVerificationStateBottomSheetDialogFragment.newInstance(
                            tokenResponse.state,
                            tokenResponse.token,
                        ).apply {
                            onRetry = { token ->
                                binding.root.navigate(
                                    R.id.action_wallet_calculate_to_identity,
                                    Bundle().apply {
                                        putString(ARGS_TOKEN, token)
                                        putBoolean(ARGS_IS_RETRY, true)
                                    },
                                )
                            }
                        }.showNow(
                            parentFragmentManager,
                            IdentityVerificationStateBottomSheetDialogFragment.TAG,
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
            requestSession = { fiatMoneyViewModel.fetchSessionsSuspend(listOf(ROUTE_API_BOT_USER_ID)) },
        )
    }
}
