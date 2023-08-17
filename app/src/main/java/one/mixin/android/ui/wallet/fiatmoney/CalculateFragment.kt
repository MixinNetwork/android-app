package one.mixin.android.ui.wallet.fiatmoney

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.FragmentCalculateBinding
import one.mixin.android.extension.clickVibrate
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.navigate
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.shaking
import one.mixin.android.extension.tickVibrate
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.AssetListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.FiatListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.TransactionsFragment.Companion.ARGS_ASSET
import one.mixin.android.ui.wallet.fiatmoney.OrderConfirmFragment.Companion.ARGS_AMOUNT
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem
import one.mixin.android.widget.Keyboard

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
                            } else {
                                val c = v + value
                                if (c.toFloat() <= maxinum) {
                                    v = c
                                } else {
                                    info.shaking()
                                }
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
            continueTv.setOnClickListener {
                // Todo check kyc
                // view.navigate(
                //     R.id.action_wallet_to_identity,
                // )
                view.navigate(
                    R.id.action_wallet_calculate_to_payment,
                    Bundle().apply {
                        putParcelable(ARGS_ASSET, asset)
                        putParcelable(ARGS_CURRENCY, currency)
                        putInt(ARGS_AMOUNT, v.toIntOrNull() ?: 0)
                    },
                )
            }
            switchIv.setOnClickListener {
                toast("todo")
            }
        }
    }

    private var rate = 0.9f
    private var v = "0"
    private var mininum = 60
    private var maxinum = 50000
    private fun updateUI() {
        binding.apply {
            assetName.text = asset.symbol
            fiatName.text = currency.name
            primaryUnit.text = currency.name
        }
        updateValue()
    }

    @SuppressLint("SetTextI18n")
    private fun updateValue() {
        binding.apply {
            val currentValue = v.toFloat()
            if (v == "0") {
                primaryTv.text = "0"
                minorTv.text = "0 ${currency.symbol}"
            } else {
                primaryTv.text = v
                minorTv.text = "≈ ${String.format("%.2f", currentValue * rate)} ${asset.symbol}"
            }
            continueTv.isEnabled = currentValue >= mininum && currentValue <= maxinum
            if (currentValue > maxinum) {
                info.setTextColor(requireContext().getColorStateList(R.color.colorRed))
            } else {
                info.setTextColor(requireContext().colorFromAttribute(R.attr.text_minor))
            }
        }
    }
}
