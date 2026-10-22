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

    private val showLitecoinTitle: Boolean by lazy {
        requireArguments().getBoolean(ARGS_SHOW_LITECOIN_TITLE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.introImage.setImageResource(
            when {
                showLitecoinTitle -> R.drawable.bg_missing_litecoin
                showPearlTitle -> R.drawable.bg_missing_pearl
                else -> R.drawable.bg_missing_btc
            },
        )
        binding.introTitle.text = getString(
            R.string.common_wallet_now_supports_chain,
            getString(when {
                showLitecoinTitle -> R.string.Litecoin
                showPearlTitle -> R.string.Pearl
                else -> R.string.Bitcoin
            }),
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
        private const val ARGS_SHOW_LITECOIN_TITLE = "args_show_litecoin_title"

        fun newInstance(showPearlTitle: Boolean, showLitecoinTitle: Boolean = false): WalletMissingBtcAddressFragment =
            WalletMissingBtcAddressFragment().withArgs {
                putBoolean(ARGS_SHOW_PEARL_TITLE, showPearlTitle)
                putBoolean(ARGS_SHOW_LITECOIN_TITLE, showLitecoinTitle)
            }
    }
}
