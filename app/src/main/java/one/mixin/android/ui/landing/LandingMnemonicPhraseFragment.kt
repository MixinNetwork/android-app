package one.mixin.android.ui.landing

import android.os.Bundle
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.crypto.mnemonicChecksumIndex
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.navTo
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.landing.components.MnemonicPhraseInput
import one.mixin.android.ui.landing.components.MnemonicState
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class LandingMnemonicPhraseFragment : BaseFragment(R.layout.fragment_landing_mnemonic_phrase) {
    companion object {
        const val TAG: String = "MnemonicPhraseFragment"

        fun newInstance(
        ): LandingMnemonicPhraseFragment =
            LandingMnemonicPhraseFragment().apply {

            }
    }

    private val binding by viewBinding(FragmentComposeBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.leftIb.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.compose.setContent {
            MnemonicPhraseInput(MnemonicState.Input, onComplete = {
                val list = ArrayList<String>()
                list.addAll(it)
                if (list.size == 13 && list[mnemonicChecksumIndex(list.subList(0, 12))] == list[12]) {
                    navTo(MnemonicPhraseFragment.newInstance(list), MnemonicPhraseFragment.TAG)
                } else {
                    toast(R.string.Invalid_mnemonic)
                }
            }
            )
        }
    }
}