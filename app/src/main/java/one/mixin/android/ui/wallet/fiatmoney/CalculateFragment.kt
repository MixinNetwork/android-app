package one.mixin.android.ui.wallet.fiatmoney

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.request.TickerRequest
import one.mixin.android.databinding.FragmentCalculateBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.navigate
import one.mixin.android.extension.openUrl
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
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.ui.wallet.fiatmoney.OrderConfirmFragment.Companion.ARGS_AMOUNT
import one.mixin.android.ui.wallet.fiatmoney.OrderConfirmFragment.Companion.ARGS_CURRENCY
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.sumsub.KycState
import one.mixin.android.widget.Keyboard

@AndroidEntryPoint
class CalculateFragment : BaseFragment(R.layout.fragment_calculate) {
    companion object {
        const val TAG = "CalculateFragment"

        fun newInstance() = CalculateFragment()
    }

    private val binding by viewBinding(FragmentCalculateBinding::bind)
    private val walletViewModel by viewModels<WalletViewModel>()
    private lateinit var asset: AssetItem
    private lateinit var currency: Currency

    private suspend fun initData() {
        val currencyList = getCurrencyData(requireContext().resources)
        currency = currencyList.find {
            it.name == Session.getFiatCurrency()
        } ?: currencyList.first()
        asset = walletViewModel.findAssetsByIds(listOf("4d8c508b-91c5-375b-92b0-ee702ed2dac5", "9b180ab6-6abe-3dc0-a13f-04169eb34bfa")).first()
    }

    private suspend fun refresh() {
        showLoading()
        handleMixinResponse(
            invokeNetwork = {
                walletViewModel.ticker(TickerRequest(0, currency.name, asset.assetId))
            },
            endBlock = {
                dismissLoading()
            },
            successBlock = {
                if (it.isSuccess) {
                    minimun = it.data?.minimun?.toIntOrNull() ?: 0
                    maxinum = it.data?.maximum?.toIntOrNull() ?: 0
                    fiatPrice = it.data?.price?.toFloatOrNull() ?: 0f
                }
            },
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            initData()
            binding.apply {
                titleView.leftIb.setOnClickListener {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
                titleView.setSubTitle(getString(R.string.Buy), "")
                titleView.rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.EMERGENCY) }
                assetRl.setOnClickListener {
                    AssetListBottomSheetDialogFragment.newInstance(
                        false,
                        arrayListOf(
                            "4d8c508b-91c5-375b-92b0-ee702ed2dac5", // USDT
                            "9b180ab6-6abe-3dc0-a13f-04169eb34bfa", // USDC
                        ),
                    ).setOnAssetClick { asset ->
                        this@CalculateFragment.asset = asset
                        updateUI()
                    }.showNow(parentFragmentManager, AssetListBottomSheetDialogFragment.TAG)
                }
                fiatRl.setOnClickListener {
                    FiatListBottomSheetDialogFragment.newInstance(currency).apply {
                        callback = object : FiatListBottomSheetDialogFragment.Callback {
                            override fun onCurrencyClick(currency: Currency) {
                                this@CalculateFragment.lifecycleScope.launch {
                                    this@CalculateFragment.currency = currency
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
                                } else if (value == "." && v.contains(".")) {
                                    // do noting
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
                )
                continueVa.setOnClickListener {
                    // checkKyc {
                    val amount = AmountUtil.toAmount(v, currency.name)
                    if (amount == null) {
                        toast("number error")
                    } else {
                        view.navigate(
                            R.id.action_wallet_calculate_to_payment,
                            Bundle().apply {
                                putParcelable(ARGS_ASSET, asset)
                                putParcelable(ARGS_CURRENCY, currency)
                                putInt(ARGS_AMOUNT, amount)
                            },
                        )
                    }
                    // }
                }
                switchIv.setOnClickListener {
                    isReverse = !isReverse
                    updateUI()
                }
            }
            refresh()
            updateUI()
            binding.info.text = getString(R.string.Value_info, minimun, currency.name, maxinum, currency.name)
        }
    }

    private var isReverse = false

    private var fiatPrice = 1f
    private var v = "0"
    private var minimun = 15
    private var maxinum = 1000
    private fun updateUI() {
        if (!isAdded) return
        if (isReverse) {
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

    private fun showLoading() {
        loading.showNow(parentFragmentManager, LoadingProgressDialogFragment.TAG)
    }

    private fun dismissLoading() {
        loading.dismiss()
    }

    @SuppressLint("SetTextI18n")
    private fun updateValue() {
        if (!isAdded) return
        binding.apply {
            val value = if (v.endsWith(".")) {
                v.substring(0, v.length)
            } else {
                v
            }
            if (isReverse) {
                val currentValue = value.toFloat() / fiatPrice
                if (value == "0") {
                    primaryTv.text = "0"
                    minorTv.text = "0 ${currency.name}"
                } else {
                    primaryTv.text = value
                    minorTv.text =
                        "≈ ${String.format("%.2f", currentValue)} ${currency.name}"
                }
                continueVa.isEnabled = currentValue >= minimun && currentValue <= maxinum
                continueTv.isEnabled = continueVa.isEnabled
                if (currentValue > maxinum) {
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
                        "≈ ${String.format("%.2f", currentValue * fiatPrice)} ${asset.symbol}"
                }
                continueVa.isEnabled = currentValue >= minimun && currentValue <= maxinum
                continueTv.isEnabled = continueVa.isEnabled
                if (currentValue > maxinum) {
                    info.setTextColor(requireContext().getColorStateList(R.color.colorRed))
                } else {
                    info.setTextColor(requireContext().colorFromAttribute(R.attr.text_minor))
                }
            }
            binding.info.text = getString(R.string.Value_info, minimun, currency.name, maxinum, currency.name)
        }
    }

    private fun checkKyc(onSuccess: () -> Unit) = lifecycleScope.launch {
        binding.continueVa.displayedChild = 1
        handleMixinResponse(
            invokeNetwork = {
                walletViewModel.token()
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
        )
    }
}
