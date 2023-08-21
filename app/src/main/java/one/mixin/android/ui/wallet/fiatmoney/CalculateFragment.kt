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
import one.mixin.android.databinding.FragmentCalculateBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.navigate
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.AssetListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.FiatListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.IdentityFragment.Companion.ARGS_IS_RETRY
import one.mixin.android.ui.wallet.IdentityVerificationStateBottomSheetDialogFragment
import one.mixin.android.ui.wallet.IdentityVerificationStateBottomSheetDialogFragment.Companion.ARGS_TOKEN
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.ui.wallet.fiatmoney.OrderConfirmFragment.Companion.ARGS_AMOUNT
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.sumsub.KycState
import one.mixin.android.widget.Keyboard
import timber.log.Timber

@AndroidEntryPoint
class CalculateFragment : BaseFragment(R.layout.fragment_calculate) {
    companion object {
        const val TAG = "CalculateFragment"
        const val ARGS_CURRENCY = "args_currency"

        fun newInstance(assetItem: AssetItem, currency: Currency) = CalculateFragment().withArgs {
            putParcelable(ARGS_ASSET, assetItem)
            putParcelable(ARGS_CURRENCY, currency)
        }
    }

    private val binding by viewBinding(FragmentCalculateBinding::bind)
    private val walletViewModel by viewModels<WalletViewModel>()
    private lateinit var asset: AssetItem
    private lateinit var currency: Currency

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        asset = requireNotNull(
            requireArguments().getParcelableCompat(
                ARGS_ASSET,
                AssetItem::class.java,
            ),
        )
        currency = requireNotNull(
            requireArguments().getParcelableCompat(
                ARGS_CURRENCY,
                Currency::class.java,
            ),
        )
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            titleView.rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.EMERGENCY) }
            updateUI()
            assetRl.setOnClickListener {
                AssetListBottomSheetDialogFragment.newInstance(false)
                    .setOnAssetClick { asset ->
                        this@CalculateFragment.asset = asset
                        updateUI()
                    }.showNow(parentFragmentManager, AssetListBottomSheetDialogFragment.TAG)
            }
            fiatRl.setOnClickListener {
                FiatListBottomSheetDialogFragment.newInstance(currency).apply {
                    callback = object : FiatListBottomSheetDialogFragment.Callback {
                        override fun onCurrencyClick(currency: Currency) {
                            this@CalculateFragment.currency = currency
                            updateUI()
                        }
                    }
                }.showNow(parentFragmentManager, FiatListBottomSheetDialogFragment.TAG)
            }
            keyboard.tipTitleEnabled = false
            keyboard.setOnClickKeyboardListener(
                object : Keyboard.OnClickKeyboardListener {
                    override fun onKeyClick(position: Int, value: String) {
                        context?.tickVibrate()
                        // todo disable float
                        if (position == 11) {
                            v = if (v == "0") {
                                "0"
                            } else if (v.length == 1) {
                                "0"
                            } else {
                                v.substring(0, v.length - 1)
                            }
                        } else {
                            if (v == "0") {
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
    }
    private var isReverse = false

    private var fiatPrice = 1f
    private var v = "0"
    private var mininum = 1
    private var maxinum = 1000
    private fun updateUI() {
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
        fiatPrice = (Fiats.getRate(currency.name) * (asset.priceUsd.toDoubleOrNull() ?: 0.toDouble())).toFloat()
        Timber.e("${Fiats.getRate(currency.name)}")
        Timber.e(asset.priceUsd)
        Timber.e("$fiatPrice")
        updateValue()
    }

    @SuppressLint("SetTextI18n")
    private fun updateValue() {
        binding.apply {
            if (isReverse) {
                val currentValue = v.toFloat() * fiatPrice
                if (v == "0") {
                    primaryTv.text = "0"
                    minorTv.text = "0 ${currency.name}"
                } else {
                    primaryTv.text = v
                    minorTv.text =
                        "≈ ${String.format("%.2f", currentValue)} ${currency.name}"
                }
                continueVa.isEnabled = currentValue >= mininum && currentValue <= maxinum
                if (currentValue > maxinum) {
                    info.setTextColor(requireContext().getColorStateList(R.color.colorRed))
                } else {
                    info.setTextColor(requireContext().colorFromAttribute(R.attr.text_minor))
                }
            } else {
                val currentValue = v.toFloat()
                if (v == "0") {
                    primaryTv.text = "0"
                    minorTv.text = "0 ${asset.symbol}"
                } else {
                    primaryTv.text = v
                    minorTv.text = "≈ ${String.format("%.2f", currentValue / fiatPrice)} ${asset.symbol}"
                }
                continueVa.isEnabled = currentValue >= mininum && currentValue <= maxinum
                if (currentValue > maxinum) {
                    info.setTextColor(requireContext().getColorStateList(R.color.colorRed))
                } else {
                    info.setTextColor(requireContext().colorFromAttribute(R.attr.text_minor))
                }
            }
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
                        val token = requireNotNull(tokenResponse.token) { "required token can not be null" }
                        view?.navigate(
                            R.id.action_wallet_calculate_to_identity,
                            Bundle().apply {
                                putString(ARGS_TOKEN, token)
                                putBoolean(ARGS_IS_RETRY, false)
                            },
                        )
                    }
                    KycState.PENDING.value, KycState.RETRY.value, KycState.BLOCKED.value -> {
                        IdentityVerificationStateBottomSheetDialogFragment.newInstance(tokenResponse.state, tokenResponse.token).apply {
                            onRetry = { token ->
                                binding.root.navigate(
                                    R.id.action_wallet_calculate_to_identity,
                                    Bundle().apply {
                                        putString(ARGS_TOKEN, token)
                                        putBoolean(ARGS_IS_RETRY, true)
                                    },
                                )
                            }
                        }.showNow(parentFragmentManager, IdentityVerificationStateBottomSheetDialogFragment.TAG)
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
