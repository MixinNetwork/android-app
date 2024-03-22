package one.mixin.android.ui.wallet.fiatmoney

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.api.request.RouteTickerRequest
import one.mixin.android.databinding.FragmentPriceExpiredBottomSheetBinding
import one.mixin.android.extension.booleanFromAttribute
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.MixinBottomSheetDialogFragment
import one.mixin.android.util.SystemUIManager
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.safe.TokenItem
import one.mixin.android.widget.BottomSheet
import timber.log.Timber

@AndroidEntryPoint
class PriceExpiredBottomSheetDialogFragment : MixinBottomSheetDialogFragment() {
    companion object {
        const val TAG = "PriceExpiredBottomSheetDialogFragment"
        private const val ARGS_AMOUNT = "args_amount"
        private const val ARGS_CURRENCY_NAME = "args_currency_name"
        private const val ARGS_ASSET = "args_asset"
        private const val ARGS_TOTAL = "args_total"
        private const val ARGS_ASSET_AMOUNT = "args_asset_amount"
        private const val ARGS_ASSET_PRICE = "args_asset_price"

        fun newInstance(
            amount: String,
            currencyName: String,
            asset: TokenItem,
            total: String,
            assetAmount: String,
            assetPrice: String,
        ) =
            PriceExpiredBottomSheetDialogFragment().withArgs {
                putString(ARGS_AMOUNT, amount)
                putString(ARGS_CURRENCY_NAME, currencyName)
                putParcelable(ARGS_ASSET, asset)
                putString(ARGS_TOTAL, total)
                putString(ARGS_ASSET_AMOUNT, assetAmount)
                putString(ARGS_ASSET_PRICE, assetPrice)
            }
    }

    private var amount: String = "0"
    private lateinit var currencyName: String
    private lateinit var asset: TokenItem
    private lateinit var total: String
    private lateinit var assetAmount: String
    private lateinit var assetPrice: String

    private val binding by viewBinding(FragmentPriceExpiredBottomSheetBinding::inflate)

    private val fiatMoneyViewModel by viewModels<FiatMoneyViewModel>()

    override fun onStart() {
        try {
            super.onStart()
        } catch (ignored: WindowManager.BadTokenException) {
        }
        dialog?.window?.let { window ->
            SystemUIManager.lightUI(
                window,
                !requireContext().booleanFromAttribute(R.attr.flag_night),
            )
        }
    }

    @SuppressLint("RestrictedApi", "SetTextI18n")
    override fun setupDialog(
        dialog: Dialog,
        style: Int,
    ) {
        super.setupDialog(dialog, style)
        contentView = binding.root
        (dialog as BottomSheet).setCustomView(contentView)
        amount = requireArguments().getString(ARGS_AMOUNT,"0")
        asset =
            requireNotNull(
                requireArguments().getParcelableCompat(
                    ARGS_ASSET,
                    TokenItem::class.java,
                ),
            )
        total = requireNotNull(requireArguments().getString(ARGS_TOTAL))
        assetAmount = requireNotNull(requireArguments().getString(ARGS_ASSET_AMOUNT))
        currencyName = requireNotNull(requireArguments().getString(ARGS_CURRENCY_NAME))
        assetPrice = requireNotNull(requireArguments().getString(ARGS_ASSET_PRICE))
        binding.apply {
            continueTv.setOnClickListener {
                dismiss()
                continueAction?.invoke(assetAmount)
            }
            cancelTv.setOnClickListener {
                dismiss()
                cancelAction?.invoke()
            }
            priceTitle.text = "${asset.symbol} ${getString(R.string.Price)}"
            assetPriceTv.text = "≈ $assetPrice $currencyName"
            assetAmountTv.text = "$assetAmount ${asset.symbol}"
            payAmount.text = total
        }
        refresh()
    }

    var continueAction: ((String) -> Unit)? = null
    var cancelAction: (() -> Unit)? = null

    @SuppressLint("SetTextI18n")
    private fun refresh() {
        lifecycleScope.launch {
            var time = 0
            while (isActive) {
                if (time == 10) {
                    val response =
                        try {
                            fiatMoneyViewModel.ticker(RouteTickerRequest(currencyName, asset.assetId, amount))
                        } catch (e: Exception) {
                            Timber.e(e)
                            continue
                        }
                    if (response.isSuccess) {
                        val ticker = response.data ?: continue
                        binding.apply {
                            assetAmount = ticker.assetAmount
                            assetPriceTv.text = "≈ ${ticker.assetPrice} $currencyName"
                            assetAmountTv.text = "${ticker.assetAmount} ${asset.symbol}"
                        }
                    }
                    time = 0
                } else {
                    delay(1000L)
                    time++
                    binding.continueTv.text = "${getString(R.string.Use_New_Price)} (${10 - time}s)"
                }
            }
        }
    }

    private val mBottomSheetBehaviorCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(
                bottomSheet: View,
                newState: Int,
            ) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    try {
                        dismissAllowingStateLoss()
                    } catch (e: IllegalStateException) {
                        Timber.w(e)
                    }
                }
            }

            override fun onSlide(
                bottomSheet: View,
                slideOffset: Float,
            ) {}
        }
}
