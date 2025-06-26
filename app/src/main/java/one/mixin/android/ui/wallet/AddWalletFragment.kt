package one.mixin.android.ui.wallet

import android.os.Bundle
import android.view.View
import one.mixin.android.R
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.landing.components.MnemonicPhrasePage
import one.mixin.android.ui.wallet.components.AddWalletPage
import one.mixin.android.util.viewBinding

class AddWalletFragment : BaseFragment(R.layout.fragment_compose) {
    companion object {
        const val TAG = "AddWalletFragment"
        fun newInstance() = AddWalletFragment()
    }

    private val binding by viewBinding(FragmentComposeBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener {
            requireActivity().finish()
        }
        binding.compose.setContent {
            AddWalletPage()
        }
    }
}

