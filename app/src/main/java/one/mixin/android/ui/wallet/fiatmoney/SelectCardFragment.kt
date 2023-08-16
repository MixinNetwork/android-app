


package one.mixin.android.ui.wallet.fiatmoney

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentSelectCardBinding
import one.mixin.android.extension.navigate
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class SelectCardFragment : BaseFragment(R.layout.fragment_select_card) {
    companion object {
        const val TAG = "SelectCardFragment"
    }

    private val binding by viewBinding(FragmentSelectCardBinding::bind)

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            titleView.setSubTitle(getString(R.string.Select_card), "")
            continueTv.setOnClickListener {
                view.navigate(R.id.action_wallet_card_to_order_confirm)
            }
        }
    }
}
