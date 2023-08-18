package one.mixin.android.ui.wallet.fiatmoney

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.request.TickerRequest
import one.mixin.android.api.response.TickerResponse
import one.mixin.android.databinding.FragmentOrderConfirmBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.navigate
import one.mixin.android.extension.openUrl
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.ui.wallet.WalletViewModel
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem

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
    private var isGooglePay: Boolean = false

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
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            titleView.rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.EMERGENCY) }
            buyVa.setOnClickListener {
                if (buyVa.displayedChild != 3) {
                    it.navigate(
                        R.id.action_wallet_confirm_to_status,
                        requireArguments(),
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
            cardNumber.text = "Visa .... 4242"
            priceTv.text = "1 USD = 0.995 USDC"
            purchaseTv.text = "48.78 USD"
            feeTv.text = "1.23 USD"
            totalTv.text = "50 USD"
        }
        refresh()
    }

    private fun refresh() {
        lifecycleScope.launch {
            while (true) {
                // Todo real
                val response = walletViewModel.ticker(TickerRequest(amount, currency.name, "4d8c508b-91c5-375b-92b0-ee702ed2dac5"))
                if (isAdded) {
                    if (response.isSuccess){
                        val ticker = response.data
                        binding.apply {
                            // Todo refresh price
                            cardNumber.text = "Visa .... 4242"
                            priceTv.text = "1 ${ticker?.currency} = ${ticker?.price} ${asset.symbol}"
                            purchaseTv.text = "${ticker?.purchase} ${ticker?.currency}"
                            feeTv.text = "${ticker?.fee} ${ticker?.currency}"
                            totalTv.text = "${ticker?.totalAmount} ${ticker?.currency}"
                        }
                    }
                } else {
                    return@launch
                }
                delay(10000L) // 10s
            }
        }
    }
}
