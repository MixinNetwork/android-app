package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentWalletMissingBtcAddressIntroBinding
import one.mixin.android.util.viewBinding
import one.mixin.android.ui.common.LoginVerifyBottomSheetDialogFragment

@AndroidEntryPoint
class WalletMissingBtcAddressFragment : Fragment(R.layout.fragment_wallet_missing_btc_address_intro) {

    interface Callback {
        fun onWalletMissingBtcAddressPinSuccess()
    }

    private val binding by viewBinding(FragmentWalletMissingBtcAddressIntroBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.unlockByPin.setOnClickListener {
            showLoginVerify()
        }
    }

    private fun showLoginVerify() {
        val fragment = parentFragmentManager.findFragmentByTag(LoginVerifyBottomSheetDialogFragment.TAG)
        if (fragment != null) return
        val dialog = LoginVerifyBottomSheetDialogFragment.newInstance()
        dialog.onDismissCallback = { isSuccess: Boolean ->
            activity?.onBackPressedDispatcher?.onBackPressed()
            if (isSuccess) {
                this@WalletMissingBtcAddressFragment.lifecycleScope.launch {
                    (activity as? Callback)?.onWalletMissingBtcAddressPinSuccess()
                }
            }
        }
        dialog.showNow(parentFragmentManager, LoginVerifyBottomSheetDialogFragment.TAG)
    }

    companion object {
        const val TAG: String = "WalletMissingBtcAddressFragment"

        fun newInstance(): WalletMissingBtcAddressFragment {
            return WalletMissingBtcAddressFragment()
        }
    }
}
