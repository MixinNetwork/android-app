package one.mixin.android.ui.wallet.fiatmoney

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue.COMPLEX_UNIT_SP
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wallet.button.ButtonOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.request.RouteTickerRequest
import one.mixin.android.databinding.FragmentOrderConfirmBinding
import one.mixin.android.extension.buildAmountSymbol
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.navigate
import one.mixin.android.extension.numberFormat
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.VerifyBottomSheetDialogFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.ui.wallet.fiatmoney.OrderStatusFragment.Companion.ARGS_INFO
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.cardIcon
import one.mixin.android.vo.safe.TokenItem
import timber.log.Timber

@AndroidEntryPoint
class OrderConfirmFragment : BaseFragment(R.layout.fragment_order_confirm) {
    companion object {
        const val TAG = "OrderConfirmFragment"
        const val ARGS_CURRENCY = "args_currency"
        const val ARGS_GOOGLE_PAY = "args_google_pay"
        const val ARGS_SCHEME = "args_scheme"
        const val ARGS_LAST = "args_last4"
        const val ARGS_INSTRUMENT_ID = "args_instrument_id"
        const val ARGS_AMOUNT = "args_amount"

        fun newInstance(
            tokenItem: TokenItem,
            currency: Currency,
        ) =
            OrderConfirmFragment().withArgs {
                putParcelable(TransactionsFragment.ARGS_ASSET, tokenItem)
                putParcelable(ARGS_CURRENCY, currency)
            }
    }

    private val binding by viewBinding(FragmentOrderConfirmBinding::bind)
    private val fiatMoneyViewModel by viewModels<FiatMoneyViewModel>()
    private lateinit var asset: TokenItem
    private var amount: Long = 0
    private lateinit var currency: Currency
    private var scheme: String? = null
    private var last4: String? = null
    private var isGooglePay: Boolean = false

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        asset =
            requireNotNull(
                requireArguments().getParcelableCompat(
                    TransactionsFragment.ARGS_ASSET,
                    TokenItem::class.java,
                ),
            )
        amount = requireArguments().getLong(ARGS_AMOUNT)
        currency =
            requireNotNull(
                requireArguments().getParcelableCompat(
                    ARGS_CURRENCY,
                    Currency::class.java,
                ),
            )
        isGooglePay =
            requireArguments().getBoolean(
                ARGS_GOOGLE_PAY,
                false,
            )
        scheme = requireArguments().getString(ARGS_SCHEME)
        last4 = requireArguments().getString(ARGS_LAST)
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            titleView.rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.EMERGENCY) }
            titleView.rightAnimator.setOnClickListener { }
            titleView.setSubTitle(getString(R.string.Order_Confirm), "")
            buyVa.displayedChild = 2
            assetAvatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
            assetAvatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                payWith,
                8,
                14,
                1,
                COMPLEX_UNIT_SP,
            )
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                priceTv,
                8,
                14,
                1,
                COMPLEX_UNIT_SP,
            )
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                feeTv,
                8,
                14,
                1,
                COMPLEX_UNIT_SP,
            )
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                feeMixinTv,
                8,
                14,
                1,
                COMPLEX_UNIT_SP,
            )
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                tokenTv,
                8,
                14,
                1,
                COMPLEX_UNIT_SP,
            )
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                purchaseTotalTv,
                8,
                14,
                1,
                COMPLEX_UNIT_SP,
            )
            buyVa.isEnabled = false
            try {
                val allowedPaymentMethods =
                    """
                    [
                        {
                            "type": "CARD",
                            "parameters": {
                                "allowedAuthMethods": ["PAN_ONLY","CRYPTOGRAM_3DS"],
                                "allowedCardNetworks": ["AMEX", "JCB", "MASTERCARD", "VISA"]
                            }
                        }
                    ]
                    """.trimIndent()
                googlePayButton.initialize(
                    ButtonOptions.newBuilder()
                        .setAllowedPaymentMethods(allowedPaymentMethods)
                        .build(),
                )
                googlePayButton.setOnClickListener {
                    googlePayButton.isClickable = false
                    showVerify(it)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
            continueTv.setOnClickListener(::showVerify)
            payWith.text =
                if (isGooglePay) {
                    "Google Pay"
                } else {
                    "${scheme?.capitalize()}...$last4"
                }
            val logo =
                when {
                    isGooglePay -> AppCompatResources.getDrawable(requireContext(), R.drawable.ic_google_pay_small)
                    else -> AppCompatResources.getDrawable(requireContext(), cardIcon(scheme))
                }.also {
                    if (isGooglePay) {
                        it?.setBounds(0, 0, 28.dp, 14.dp)
                    } else {
                        it?.setBounds(0, 0, 26.dp, 16.dp)
                    }
                }
            // place, not display
            setAssetAmount("1")
            payWith.setCompoundDrawables(logo, null, null, null)
            priceTv.text = info.exchangeRate
            purchaseTotalTv.text = info.purchase
            tokenTv.text = info.purchase
            feeTv.text = info.feeByGateway
            feeMixinTv.text = info.feeByMixin
            purchaseTotalTv.text = info.purchaseTotal
        }
        refresh()
    }

    private fun showVerify(view: View) {
        VerifyBottomSheetDialogFragment.newInstance(getString(R.string.Verify_PIN), true).apply {
            disableToast = true
        }.setOnPinSuccess { _ ->
            view.navigate(
                R.id.action_wallet_confirm_to_status,
                requireArguments().apply {
                    putParcelable(ARGS_INFO, info)
                },
            )
        }.showNow(parentFragmentManager, VerifyBottomSheetDialogFragment.TAG)
    }

    private val calculating = MixinApplication.appContext.getString(R.string.calculating)

    private var info =
        OrderInfo(
            "null",
            calculating,
            calculating,
            calculating,
            calculating,
            calculating,
            "",
        )

    @SuppressLint("SetTextI18n")
    private fun refresh() {
        lifecycleScope.launch {
            var time = 10
            while (isActive) {
                if (time == 10) {
                    val response =
                        try {
                            fiatMoneyViewModel.ticker(RouteTickerRequest(currency.name, asset.assetId, amount.toString()))
                        } catch (e: Exception) {
                            Timber.e(e)
                            continue
                        }
                    if (viewDestroyed()) return@launch

                    if (response.isSuccess) {
                        val ticker = response.data ?: continue
                        info =
                            OrderInfo(
                                "$scheme...$last4",
                                "1 ${asset.symbol} ≈ ${ticker.assetPrice} ${currency.name}",
                                "${ticker.purchase} ${ticker.currency}",
                                "${ticker.feeByGateway} ${ticker.currency}",
                                "${ticker.feeByMixin} ${ticker.currency}",
                                "${ticker.totalAmount} ${ticker.currency}",
                                ticker.assetAmount,
                            )
                        binding.apply {
                            priceTv.text = info.exchangeRate
                            feeTv.text = info.feeByGateway
                            feeMixinTv.text = info.feeByMixin
                            tokenTv.text = "${info.assetAmount} ${asset.symbol}"
                            purchaseTotalTv.text = info.purchaseTotal
                            setAssetAmount(info.assetAmount)
                            assetName.isVisible = true
                            if (!buyVa.isEnabled) {
                                buyVa.isEnabled = true
                                buyVa.displayedChild =
                                    if (isGooglePay) {
                                        1
                                    } else {
                                        0
                                    }
                            }
                        }
                    }
                    time = 0
                } else {
                    delay(1000L)
                    time++
                    if (!viewDestroyed()) {
                        binding.timeTv.text = "${10 - time}s"
                    }
                }
            }
        }
    }

    private fun setAssetAmount(amount: String) {
        val amountVal = amount.toFloatOrNull()
        val isPositive = if (amountVal == null) false else amountVal > 0
        val amountText =
            if (isPositive) {
                "+${amount.numberFormat()}"
            } else {
                amount.numberFormat()
            }
        val amountColor =
            resources.getColor(
                when {
                    isPositive -> {
                        R.color.wallet_green
                    }
                    else -> {
                        R.color.wallet_pink
                    }
                },
                null,
            )
        val symbolColor = requireContext().colorFromAttribute(R.attr.text_primary)
        binding.assetName.text = buildAmountSymbol(requireContext(), amountText, asset.symbol, amountColor, symbolColor)
    }
}
