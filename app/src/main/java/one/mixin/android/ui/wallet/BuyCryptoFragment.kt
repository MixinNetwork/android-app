package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.checkout.base.model.Environment
import com.checkout.frames.api.PaymentFlowHandler
import com.checkout.frames.api.PaymentFormMediator
import com.checkout.frames.screen.paymentform.PaymentFormConfig
import com.checkout.frames.style.screen.PaymentFormStyle
import com.checkout.tokenization.model.TokenDetails
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig.CHCEKOUT_ID
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
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.common.VerifyFragment
import one.mixin.android.util.viewBinding
import one.mixin.android.vo.Account
import one.mixin.android.vo.User
import one.mixin.android.vo.checkout.TraceRequest
import one.mixin.android.vo.sumsub.State
import timber.log.Timber

@AndroidEntryPoint
class BuyCryptoFragment : BaseFragment(R.layout.fragment_buy_crypto) {
    companion object {
        const val TAG = "IdentityFragment"

        fun newInstance() = IdentityFragment()
    }

    private val binding by viewBinding(FragmentBuyCryptoBinding::bind)
    private val walletViewModel by viewModels<WalletViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            titleView.leftIb.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
            titleView.rightAnimator.setOnClickListener { context?.openUrl(Constants.HelpLink.EMERGENCY) }
            continueTv.setOnClickListener {
                checkToken()
            }
        }
    }

    private fun checkToken()  = lifecycleScope.launch{
        binding.innerVa.displayedChild = 1
        // Todo
        navTo(PaymentFragment(), PaymentFragment.TAG)
    }

    private fun placeOrder(token: String) = lifecycleScope.launch {
        val response = walletViewModel.payment(
            TraceRequest(
                token, "USD",
                requireNotNull(Session.getAccountId()),
                1,
                "965e5c6e-434c-3fa9-b780-c50f43cd955c"
            )
        )
        binding.innerVa.displayedChild = 0
        Timber.e(response.traceID)
    }
}
