package one.mixin.android.ui.setting

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentComposeBinding
import one.mixin.android.extension.navTo
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.setting.ui.page.RecoveryKitPage
import one.mixin.android.util.viewBinding

@AndroidEntryPoint
class RecoveryFragment : BaseFragment(R.layout.fragment_compose) {
    companion object {
        const val TAG: String = "MnemonicPhraseFragment"

        fun newInstance(
        ): RecoveryFragment =
            RecoveryFragment().apply {

            }
    }

    private val binding by viewBinding(FragmentComposeBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.isVisible = false
        binding.titleView.leftIb.setOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        binding.compose.setContent {
            RecoveryKitPage({
                navTo(AddPhoneFragment.newInstance(), AddPhoneFragment.TAG)
            }, {
                navTo(MnemonicPhraseBackupFragment.newInstance(), MnemonicPhraseBackupFragment.TAG)
            }, {
                navTo(EmergencyContactFragment.newInstance(), EmergencyContactFragment.TAG)
            })
        }
    }
}