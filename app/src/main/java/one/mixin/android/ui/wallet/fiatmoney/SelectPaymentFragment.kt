package one.mixin.android.ui.wallet.fiatmoney

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSelectPaymentBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.navigate
import one.mixin.android.extension.round
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem

@AndroidEntryPoint
class SelectPaymentFragment : BaseFragment(R.layout.fragment_select_payment) {
    companion object {
        const val TAG = "SelectPaymentFragment"
    }

    private val binding by viewBinding(FragmentSelectPaymentBinding::bind)

    private lateinit var asset: AssetItem
    private lateinit var currency: Currency

    private val fiatMoneyViewModel by viewModels<FiatMoneyViewModel>()

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        asset = requireNotNull(
            requireArguments().getParcelableCompat(
                TransactionsFragment.ARGS_ASSET,
                AssetItem::class.java,
            ),
        )
        currency = requireNotNull(
            requireArguments().getParcelableCompat(
                OrderConfirmFragment.ARGS_CURRENCY,
                Currency::class.java,
            ),
        )
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            titleView.setSubTitle(getString(R.string.Select_Payment), "")
            firstRl.round(8.dp)
            secondRl.round(8.dp)
            firstRl.isVisible = !fiatMoneyViewModel.hideGooglePay
            firstRl.setOnClickListener {
                if (fiatMoneyViewModel.state.value.googlePayAvailable != true) {
                    // toast(R.string.Google_Pay_error)
                    // return@setOnClickListener
                }
                view.navigate(
                    R.id.action_wallet_payment_to_order_confirm,
                    requireArguments().apply {
                        putBoolean(OrderConfirmFragment.ARGS_GOOGLE_PAY, true)
                    },
                )
            }
            secondRl.setOnClickListener {
                view.navigate(
                    R.id.action_wallet_payment_to_select_card,
                    requireArguments(),
                )
            }
        }
    }
}
