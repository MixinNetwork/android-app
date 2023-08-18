package one.mixin.android.ui.wallet.fiatmoney

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.Constants.TEST_ASSET_ID
import one.mixin.android.R
import one.mixin.android.api.request.TickerRequest
import one.mixin.android.databinding.FragmentOrderConfirmBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.navigate
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.ui.wallet.fiatmoney.OrderStatusFragment.Companion.ARGS_INFO
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem
import timber.log.Timber

@AndroidEntryPoint
class OrderConfirmFragment : BaseFragment(R.layout.fragment_order_confirm) {
    companion object {
        const val TAG = "OrderConfirmFragment"
        const val ARGS_CURRENCY = "args_currency"
        const val ARGS_GOOGLE_PAY = "args_google_pay"
        const val ARGS_SCHEME = "args_scheme"
        const val ARGS_INSTRUMENT_ID = "args_instrument_id"
        const val ARGS_AMOUNT = "args_amount"

        fun newInstance(assetItem: AssetItem, currency: Currency) =
            OrderConfirmFragment().withArgs {
                putParcelable(TransactionsFragment.ARGS_ASSET, assetItem)
                putParcelable(ARGS_CURRENCY, currency)
            }
    }

    private val binding by viewBinding(FragmentOrderConfirmBinding::bind)
    private val walletViewModel by viewModels<WalletViewModel>()
    private lateinit var asset: AssetItem
    private var amount: Int = 0
    private lateinit var currency: Currency
    private var scheme: String? = null
    private var isGooglePay: Boolean = false

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        asset = requireNotNull(
            requireArguments().getParcelableCompat(
                TransactionsFragment.ARGS_ASSET,
                AssetItem::class.java,
            ),
        )
        amount = requireArguments().getInt(ARGS_AMOUNT)
        currency = requireNotNull(
            requireArguments().getParcelableCompat(
                ARGS_CURRENCY,
                Currency::class.java,
            ),
        )
        isGooglePay = requireArguments().getBoolean(
            ARGS_GOOGLE_PAY,
            false,
        )
        scheme = requireArguments().getString(ARGS_SCHEME)
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            titleView.rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.EMERGENCY) }
            buyVa.setOnClickListener {
                if (buyVa.displayedChild != 3) {
                    it.navigate(
                        R.id.action_wallet_confirm_to_status,
                        requireArguments().apply {
                            putParcelable(ARGS_INFO, info)
                        },
                    )
                }
            }
            titleView.rightAnimator.setOnClickListener { }
            titleView.setSubTitle(getString(R.string.Order_Confirm), "")

            val toValue = if (isGooglePay) {
                1
            } else {
                0
            }
            if (buyVa.displayedChild != toValue) {
                buyVa.displayedChild = if (isGooglePay) {
                    1
                } else {
                    0
                }

                buyVa.setBackgroundResource(
                    if (isGooglePay) {
                        R.drawable.bg_round_black_btn_40
                    } else {
                        R.drawable.bg_round_blue_btn_40
                    },
                )
            }
            assetAvatar.bg.loadImage(asset.iconUrl, R.drawable.ic_avatar_place_holder)
            assetAvatar.badge.loadImage(asset.chainIconUrl, R.drawable.ic_avatar_place_holder)
            priceRl.setOnClickListener {
                PriceExpiredBottomSheetDialogFragment.newInstance()
                    .showNow(parentFragmentManager, PriceExpiredBottomSheetDialogFragment.TAG)
            }
            feeRl.setOnClickListener {
                FeeBottomSheetDialogFragment.newInstance()
                    .showNow(parentFragmentManager, FeeBottomSheetDialogFragment.TAG)
            }

            // Todo real data
            payWith.text = if (isGooglePay) {
                "Google Pay"
            } else {
                "Visa .... 4242"
            }
            val logo = when {
                isGooglePay -> AppCompatResources.getDrawable(requireContext(), R.drawable.ic_google_pay_small)
                scheme == "mastercard" -> AppCompatResources.getDrawable(requireContext(), R.drawable.ic_mastercard)
                else -> AppCompatResources.getDrawable(requireContext(), R.drawable.ic_visa)
            }.also {
                it?.setBounds(0, 0, 28.dp, 14.dp)
            }
            payWith.setCompoundDrawables(logo, null, null, null)
            priceTv.text = info.price
            purchaseTv.text = info.purchase
            feeTv.text = info.fee
            totalTv.text = info.total
        }
        refresh()
    }

    // Todo real data
    private var info = OrderInfo(
        "1 USD = 0.995 USDC",
        "48.78 USD",
        "1.23 USD",
        "50 USD",
    )

    @SuppressLint("SetTextI18n")
    private fun refresh() {
        lifecycleScope.launch {
            var time = 0
            while (true) {
                if (time == 10) {
                    val response = try {
                        walletViewModel.ticker(TickerRequest(amount, currency.name, TEST_ASSET_ID))
                    } catch (e: Exception) {
                        Timber.e(e)
                        return@launch
                    }
                    if (isAdded) {
                        if (response.isSuccess) {
                            val ticker = response.data
                            info = OrderInfo(
                                "1 ${ticker?.currency} = ${ticker?.price} ${asset.symbol}",
                                "${ticker?.purchase} ${ticker?.currency}",
                                "${ticker?.fee} ${ticker?.currency}",
                                "${ticker?.totalAmount} ${ticker?.currency}",
                            )
                            binding.apply {
                                priceTv.text = info.price
                                purchaseTv.text = info.purchase
                                feeTv.text = info.fee
                                totalTv.text = info.total
                            }
                        }
                    } else {
                        return@launch
                    }
                    time = 0
                } else {
                    delay(1000L)
                    time++
                    if (isAdded) {
                        binding.timeTv.text = "${10 - time}s"
                    }
                }
            }
        }
    }
}
