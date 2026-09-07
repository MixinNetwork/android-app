package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.databinding.FragmentWalletMissingBtcAddressIntroBinding
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.LoginVerifyBottomSheetDialogFragment
import one.mixin.android.ui.landing.reuseOrCreateLoginPinGate
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class WalletMissingBtcAddressFragment : Fragment(R.layout.fragment_wallet_missing_btc_address_intro) {

    interface Callback {
        fun onWalletMissingBtcAddressPinSuccess()
    }

    private val binding by viewBinding(FragmentWalletMissingBtcAddressIntroBinding::bind)

    private val showPearlTitle: Boolean by lazy {
        requireArguments().getBoolean(ARGS_SHOW_PEARL_TITLE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.introImage.setImageResource(
            if (showPearlTitle) R.drawable.bg_missing_pearl else R.drawable.bg_missing_btc,
        )
        binding.introTitle.setText(
            if (showPearlTitle) R.string.classic_wallet_pearl_intro_title else R.string.classic_wallet_btc_intro_title,
        )
        binding.unlockByPin.setOnClickListener {
            showLoginVerify()
        }
        findLoginVerify()?.let(::bindLoginVerify)
    }

    private fun showLoginVerify() {
        reuseOrCreateLoginPinGate(
            existing = findLoginVerify(),
            create = LoginVerifyBottomSheetDialogFragment::newInstance,
            bind = ::bindLoginVerify,
            show = { dialog ->
                dialog.showNow(parentFragmentManager, LoginVerifyBottomSheetDialogFragment.TAG)
            },
        )
    }

    private fun findLoginVerify(): LoginVerifyBottomSheetDialogFragment? =
        parentFragmentManager.findFragmentByTag(LoginVerifyBottomSheetDialogFragment.TAG) as? LoginVerifyBottomSheetDialogFragment

    private fun bindLoginVerify(dialog: LoginVerifyBottomSheetDialogFragment) {
        dialog.onDismissCallback = { isSuccess: Boolean, _ ->
            if (isSuccess) {
                this@WalletMissingBtcAddressFragment.lifecycleScope.launch {
                    (activity as? Callback)?.onWalletMissingBtcAddressPinSuccess()
                }
            }
        }
    }

    companion object {
        const val TAG: String = "WalletMissingBtcAddressFragment"
        private const val ARGS_SHOW_PEARL_TITLE = "args_show_pearl_title"

        fun newInstance(showPearlTitle: Boolean): WalletMissingBtcAddressFragment =
            WalletMissingBtcAddressFragment().withArgs {
                putBoolean(ARGS_SHOW_PEARL_TITLE, showPearlTitle)
            }
    }
}
