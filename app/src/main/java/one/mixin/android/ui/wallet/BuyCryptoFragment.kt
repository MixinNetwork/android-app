package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.databinding.FragmentBuyBottomBinding
import one.mixin.android.databinding.FragmentBuyCryptoBinding
import one.mixin.android.databinding.FragmentEmergencyContactBinding
import one.mixin.android.databinding.FragmentIdentityBinding
import one.mixin.android.extension.alertDialogBuilder
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.navTo
import one.mixin.android.extension.openUrl
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Account
import one.mixin.android.vo.User

@AndroidEntryPoint
class BuyCryptoFragment : BaseFragment(R.layout.fragment_buy_crypto) {
    companion object {
        const val TAG = "IdentityFragment"

        fun newInstance() = IdentityFragment()
    }

    private val binding by viewBinding(FragmentBuyCryptoBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            titleView.rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.EMERGENCY) }
            continueTv.setOnClickListener {
                // Todo
            }
        }
    }
}
