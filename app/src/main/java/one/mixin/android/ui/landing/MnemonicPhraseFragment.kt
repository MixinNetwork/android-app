package one.mixin.android.ui.landing

import MnemonicPhraseInput
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentMnemonicPhraseBinding
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class MnemonicPhraseFragment: BaseFragment(R.layout.fragment_mnemonic_phrase) {
    companion object {
        const val TAG: String = "MnemonicPhraseFragment"

        fun newInstance(
        ): MnemonicPhraseFragment =
            MnemonicPhraseFragment().apply {

            }
    }

    private val mobileViewModel by viewModels<MobileViewModel>()
    private val binding by viewBinding(FragmentMnemonicPhraseBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.compose.setContent {
            MnemonicPhraseInput{}
        }
    }
}