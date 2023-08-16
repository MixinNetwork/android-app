


package one.mixin.android.ui.wallet.fiatmoney

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSelectCardBinding
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.navigate
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.Currency
import one.mixin.android.ui.wallet.BuyCryptoFragment
import one.mixin.android.ui.wallet.TransactionsFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.AssetItem

@AndroidEntryPoint
class SelectCardFragment : BaseFragment(R.layout.fragment_select_card) {
    companion object {
        const val TAG = "SelectCardFragment"
    }

    private val binding by viewBinding(FragmentSelectCardBinding::bind)
    private lateinit var asset: AssetItem
    private lateinit var currency: Currency

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
            titleView.setSubTitle(getString(R.string.Select_Card), "")
            continueTv.setOnClickListener {
                view.navigate(
                    R.id.action_wallet_card_to_order_confirm,
                    Bundle().apply {
                        putParcelable(TransactionsFragment.ARGS_ASSET, asset)
                        putParcelable(BuyCryptoFragment.ARGS_CURRENCY, currency)
                    },
                )
            }
        }
    }
}
